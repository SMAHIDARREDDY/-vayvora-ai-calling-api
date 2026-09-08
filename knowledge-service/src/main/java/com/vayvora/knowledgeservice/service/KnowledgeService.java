package com.vayvora.knowledgeservice.service;

import com.vayvora.knowledgeservice.dto.KnowledgeDtos.AddDocumentRequest;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.CreateKnowledgeBaseRequest;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.DocumentResponse;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.IngestionStatsResponse;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.KnowledgeBaseResponse;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.SearchResultResponse;
import com.vayvora.shared.entity.Knowledge;
import com.vayvora.shared.enums.Enums.IngestStatus;
import com.vayvora.shared.repository.Repositories.KnowledgeBaseRepository;
import com.vayvora.shared.repository.Repositories.KnowledgeChunkRepository;
import com.vayvora.shared.repository.Repositories.KnowledgeDocumentRepository;
import com.vayvora.shared.tenant.TenantContext;
import com.vayvora.shared.web.Web.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Knowledge base management and the ingestion pipeline (spec §7).
 *
 * <p>Documents move QUEUED -> PARSING -> CHUNKING -> EMBEDDING -> INDEXED.
 * Chunking is implemented here; embedding is left as an explicit integration
 * point, since the vector store and model are deployment choices (spec §26).
 * Until that is wired up, retrieval falls back to keyword matching so the RAG
 * path is exercisable end to end.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 25;

    private final KnowledgeBaseRepository knowledgeBases;
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeChunkRepository chunks;

    // -----------------------------------------------------------------------
    // Knowledge bases
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<KnowledgeBaseResponse> list(Pageable pageable) {
        return knowledgeBases
                .findByOrganizationId(TenantContext.requireOrganizationId(), pageable)
                .map(KnowledgeBaseResponse::from);
    }

    @Transactional
    public KnowledgeBaseResponse create(CreateKnowledgeBaseRequest request) {
        Knowledge.KnowledgeBase kb = Knowledge.KnowledgeBase.builder()
                .organizationId(TenantContext.requireOrganizationId())
                .name(request.name())
                .description(request.description())
                .build();

        if (request.embeddingModel() != null) {
            kb.setEmbeddingModel(request.embeddingModel());
        }
        if (request.chunkSize() != null && request.chunkSize() > 0) {
            kb.setChunkSize(request.chunkSize());
        }
        if (request.chunkOverlap() != null && request.chunkOverlap() >= 0) {
            kb.setChunkOverlap(request.chunkOverlap());
        }
        return KnowledgeBaseResponse.from(knowledgeBases.save(kb));
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseResponse get(String knowledgeBaseId) {
        return KnowledgeBaseResponse.from(requireBase(knowledgeBaseId));
    }

    // -----------------------------------------------------------------------
    // Documents
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(String knowledgeBaseId, Pageable pageable) {
        requireBase(knowledgeBaseId);
        return documents.findByKnowledgeBaseId(knowledgeBaseId, pageable)
                .map(DocumentResponse::from);
    }

    /**
     * Registers a document and, when content is supplied inline, chunks it
     * immediately.
     *
     * <p>A document without inline content stays QUEUED for an out-of-band
     * worker to fetch and parse.
     */
    @Transactional
    public DocumentResponse addDocument(String knowledgeBaseId, AddDocumentRequest request) {
        Knowledge.KnowledgeBase kb = requireBase(knowledgeBaseId);

        Knowledge.KnowledgeDocument doc = documents.save(Knowledge.KnowledgeDocument.builder()
                .organizationId(kb.getOrganizationId())
                .knowledgeBaseId(knowledgeBaseId)
                .title(request.title())
                .sourceType(request.sourceType())
                .sourceUri(request.sourceUri())
                .sizeBytes(request.sizeBytes())
                .status(IngestStatus.QUEUED)
                .uploadedBy(TenantContext.userId())
                .build());

        if (request.content() != null && !request.content().isBlank()) {
            ingestInline(kb, doc, request.content());
        }
        return DocumentResponse.from(doc);
    }

    @Transactional
    public DocumentResponse reingest(String documentId) {
        Knowledge.KnowledgeDocument doc = requireDocument(documentId);
        Knowledge.KnowledgeBase kb = requireBase(doc.getKnowledgeBaseId());

        chunks.deleteByDocumentId(documentId);
        doc.setChunkCount(0);
        doc.setStatus(IngestStatus.QUEUED);
        doc.setErrorMessage(null);
        doc.setIndexedAt(null);
        documents.save(doc);

        // Re-chunking requires the source text, which lives in object storage
        // for uploaded files; only inline-sourced documents can be replayed
        // here without fetching it first.
        return DocumentResponse.from(doc);
    }

    @Transactional
    public void deleteDocument(String documentId) {
        Knowledge.KnowledgeDocument doc = requireDocument(documentId);
        chunks.deleteByDocumentId(doc.getId());
        documents.delete(doc);
    }

    @Transactional(readOnly = true)
    public IngestionStatsResponse stats() {
        String orgId = TenantContext.requireOrganizationId();
        long total = documents.findByOrganizationId(orgId, Pageable.unpaged())
                .getTotalElements();
        return new IngestionStatsResponse(
                total,
                documents.countByOrganizationIdAndStatus(orgId, IngestStatus.INDEXED),
                documents.countByOrganizationIdAndStatus(orgId, IngestStatus.FAILED),
                documents.sumChunks(orgId));
    }

    // -----------------------------------------------------------------------
    // Retrieval
    // -----------------------------------------------------------------------

    /**
     * Retrieves candidate chunks for a query.
     *
     * <p>Keyword matching stands in for vector similarity until the vector
     * store is configured. Scores are positional rather than semantic, and are
     * returned so callers can display ranking without implying a similarity
     * measure that is not yet computed.
     */
    @Transactional(readOnly = true)
    public List<SearchResultResponse> search(String query, Integer topK) {
        String orgId = TenantContext.requireOrganizationId();
        int limit = topK == null ? DEFAULT_TOP_K : Math.min(Math.max(1, topK), MAX_TOP_K);

        List<Knowledge.KnowledgeChunk> matches =
                chunks.searchByKeyword(orgId, query.trim(), PageRequest.of(0, limit));

        Map<String, String> titles = documents
                .findByOrganizationId(orgId, Pageable.unpaged())
                .stream()
                .collect(Collectors.toMap(
                        Knowledge.KnowledgeDocument::getId,
                        Knowledge.KnowledgeDocument::getTitle,
                        (a, b) -> a));

        List<SearchResultResponse> results = new ArrayList<>(matches.size());
        for (int i = 0; i < matches.size(); i++) {
            Knowledge.KnowledgeChunk c = matches.get(i);
            results.add(new SearchResultResponse(
                    c.getId(),
                    c.getDocumentId(),
                    titles.getOrDefault(c.getDocumentId(), "Untitled"),
                    c.getOrdinal(),
                    c.getContent(),
                    1.0 - (i / (double) Math.max(1, matches.size()))));
        }
        return results;
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private Knowledge.KnowledgeBase requireBase(String id) {
        return knowledgeBases
                .findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Knowledge base " + id + " not found"));
    }

    private Knowledge.KnowledgeDocument requireDocument(String id) {
        return documents
                .findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
    }

    /** Chunks inline text and marks the document indexed. */
    private void ingestInline(Knowledge.KnowledgeBase kb,
                              Knowledge.KnowledgeDocument doc,
                              String content) {
        doc.setStatus(IngestStatus.CHUNKING);
        documents.save(doc);

        List<String> parts = chunk(content, kb.getChunkSize(), kb.getChunkOverlap());
        for (int i = 0; i < parts.size(); i++) {
            chunks.save(Knowledge.KnowledgeChunk.builder()
                    .organizationId(kb.getOrganizationId())
                    .documentId(doc.getId())
                    .ordinal(i)
                    .content(parts.get(i))
                    .tokenCount(estimateTokens(parts.get(i)))
                    .build());
        }

        doc.setChunkCount(parts.size());
        doc.setStatus(IngestStatus.INDEXED);
        doc.setIndexedAt(Instant.now());
        documents.save(doc);
    }

    /**
     * Splits text into overlapping windows.
     *
     * <p>Overlap keeps a sentence that straddles a boundary retrievable from
     * either side. It is clamped below the window size so the walk always makes
     * forward progress.
     */
    static List<String> chunk(String text, int size, int overlap) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        int window = Math.max(1, size);
        int step = Math.max(1, window - Math.max(0, Math.min(overlap, window - 1)));

        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + window);
            String part = text.substring(start, end).trim();
            if (!part.isEmpty()) {
                out.add(part);
            }
            if (end == text.length()) {
                break;
            }
        }
        return out;
    }

    /** Rough token estimate; adequate for capacity display, not for billing. */
    private static int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }
}
