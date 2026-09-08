package com.vayvora.knowledgeservice.dto;

import com.vayvora.shared.entity.Knowledge;
import com.vayvora.shared.enums.Enums.KnowledgeSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Request and response payloads for the knowledge API. */
public final class KnowledgeDtos {

    private KnowledgeDtos() {
    }

    public record CreateKnowledgeBaseRequest(
            @NotBlank(message = "Name is required")
            String name,
            String description,
            String embeddingModel,
            Integer chunkSize,
            Integer chunkOverlap) {
    }

    /**
     * Registers a document for ingestion.
     *
     * <p>Content may be supplied inline for text sources; a binary upload
     * instead provides {@code sourceUri} pointing at object storage.
     */
    public record AddDocumentRequest(
            @NotBlank(message = "Title is required")
            String title,

            @NotNull(message = "Source type is required")
            KnowledgeSourceType sourceType,

            String sourceUri,
            String content,
            Long sizeBytes) {
    }

    public record KnowledgeBaseResponse(
            String id,
            String organizationId,
            String name,
            String description,
            String embeddingModel,
            Integer chunkSize,
            Integer chunkOverlap,
            Instant createdAt) {

        public static KnowledgeBaseResponse from(Knowledge.KnowledgeBase kb) {
            return new KnowledgeBaseResponse(
                    kb.getId(),
                    kb.getOrganizationId(),
                    kb.getName(),
                    kb.getDescription(),
                    kb.getEmbeddingModel(),
                    kb.getChunkSize(),
                    kb.getChunkOverlap(),
                    kb.getCreatedAt());
        }
    }

    public record DocumentResponse(
            String id,
            String knowledgeBaseId,
            String title,
            String sourceType,
            String sourceUri,
            String status,
            Integer chunkCount,
            Long sizeBytes,
            String errorMessage,
            Instant indexedAt,
            Instant createdAt) {

        public static DocumentResponse from(Knowledge.KnowledgeDocument d) {
            return new DocumentResponse(
                    d.getId(),
                    d.getKnowledgeBaseId(),
                    d.getTitle(),
                    d.getSourceType().name(),
                    d.getSourceUri(),
                    d.getStatus().name(),
                    d.getChunkCount(),
                    d.getSizeBytes(),
                    d.getErrorMessage(),
                    d.getIndexedAt(),
                    d.getCreatedAt());
        }
    }

    /** A chunk returned by retrieval, with the document it came from. */
    public record SearchResultResponse(
            String chunkId,
            String documentId,
            String documentTitle,
            Integer ordinal,
            String content,
            Double score) {
    }

    public record SearchRequest(
            @NotBlank(message = "Query is required")
            String query,
            Integer topK) {
    }

    public record IngestionStatsResponse(
            long totalDocuments,
            long indexed,
            long failed,
            long totalChunks) {
    }

    public record MessageResponse(String message) {
    }

    public record SearchResponse(String query, List<SearchResultResponse> results) {
    }
}
