package com.vayvora.shared.entity;

import com.vayvora.shared.enums.Enums.IngestStatus;
import com.vayvora.shared.enums.Enums.KnowledgeSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Knowledge base and RAG tables (spec §7, §23).
 *
 * <p>A document is parsed, chunked and embedded before it can ground an answer.
 * Chunks and embeddings are separate tables because the embedding model can be
 * swapped and re-run without re-parsing source documents.
 */
public final class Knowledge {

    private Knowledge() {
    }

    @Entity
    @Table(name = "knowledge_bases", indexes = {
            @Index(name = "idx_kb_org", columnList = "organization_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KnowledgeBase extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(nullable = false)
        private String name;

        @Column(length = 512)
        private String description;

        @Column(name = "embedding_model", length = 96)
        @Builder.Default
        private String embeddingModel = "text-embedding-3-small";

        @Column(name = "chunk_size", nullable = false)
        @Builder.Default
        private Integer chunkSize = 800;

        @Column(name = "chunk_overlap", nullable = false)
        @Builder.Default
        private Integer chunkOverlap = 120;
    }

    @Entity
    @Table(name = "knowledge_documents", indexes = {
            @Index(name = "idx_kdoc_kb", columnList = "knowledge_base_id"),
            @Index(name = "idx_kdoc_org", columnList = "organization_id"),
            @Index(name = "idx_kdoc_status", columnList = "knowledge_base_id,status")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KnowledgeDocument extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "knowledge_base_id", nullable = false)
        private String knowledgeBaseId;

        @Column(nullable = false, length = 512)
        private String title;

        @Enumerated(EnumType.STRING)
        @Column(name = "source_type", nullable = false, length = 16)
        private KnowledgeSourceType sourceType;

        /** Object-storage key, or the source URL for {@code URL} documents. */
        @Column(name = "source_uri", length = 1024)
        private String sourceUri;

        @Column(name = "size_bytes")
        private Long sizeBytes;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private IngestStatus status = IngestStatus.QUEUED;

        @Column(name = "chunk_count", nullable = false)
        @Builder.Default
        private Integer chunkCount = 0;

        /** Populated when {@code status} is {@code FAILED}. */
        @Column(name = "error_message", length = 1024)
        private String errorMessage;

        @Column(name = "indexed_at")
        private Instant indexedAt;

        @Column(name = "uploaded_by")
        private String uploadedBy;
    }

    /** A retrievable span of text extracted from a document. */
    @Entity
    @Table(name = "knowledge_chunks", uniqueConstraints = {
            @UniqueConstraint(name = "uk_chunk_doc_ordinal",
                    columnNames = {"document_id", "ordinal"})
    }, indexes = {
            @Index(name = "idx_chunk_doc", columnList = "document_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KnowledgeChunk extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "document_id", nullable = false)
        private String documentId;

        /** Zero-based position of this chunk within its document. */
        @Column(nullable = false)
        private Integer ordinal;

        @Lob
        @Column(nullable = false, columnDefinition = "TEXT")
        private String content;

        @Column(name = "token_count")
        private Integer tokenCount;

        /** Free-form JSON: page number, section heading, and similar. */
        @Column(name = "metadata", columnDefinition = "TEXT")
        private String metadata;
    }

    /**
     * Vector for a chunk.
     *
     * <p>Held in its own table so the platform can move to a dedicated vector
     * store (spec §26) without disturbing chunk text. The vector is stored as
     * text here; a pgvector column is the intended production representation.
     */
    @Entity
    @Table(name = "knowledge_embeddings", indexes = {
            @Index(name = "idx_embed_chunk", columnList = "chunk_id", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KnowledgeEmbedding extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "chunk_id", nullable = false, unique = true)
        private String chunkId;

        @Column(name = "model", nullable = false, length = 96)
        private String model;

        @Column(name = "dimensions", nullable = false)
        private Integer dimensions;

        @Lob
        @Column(name = "vector", columnDefinition = "TEXT")
        private String vector;
    }
}
