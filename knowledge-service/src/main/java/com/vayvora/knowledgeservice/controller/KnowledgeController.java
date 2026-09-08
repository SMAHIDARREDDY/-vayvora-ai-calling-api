package com.vayvora.knowledgeservice.controller;

import com.vayvora.knowledgeservice.dto.KnowledgeDtos.AddDocumentRequest;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.CreateKnowledgeBaseRequest;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.DocumentResponse;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.IngestionStatsResponse;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.KnowledgeBaseResponse;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.MessageResponse;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.SearchRequest;
import com.vayvora.knowledgeservice.dto.KnowledgeDtos.SearchResponse;
import com.vayvora.knowledgeservice.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Knowledge base endpoints (spec §30). */
@RestController
@RequestMapping("/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeController {

    private static final int MAX_PAGE_SIZE = 100;

    private final KnowledgeService knowledgeService;

    @GetMapping
    public Page<KnowledgeBaseResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return knowledgeService.list(
                PageRequest.of(Math.max(0, page), clamp(size),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /** Declared before {@code /{id}} so "stats" is not read as an id. */
    @GetMapping("/stats")
    public IngestionStatsResponse stats() {
        return knowledgeService.stats();
    }

    @GetMapping("/{id}")
    public KnowledgeBaseResponse get(@PathVariable String id) {
        return knowledgeService.get(id);
    }

    @PostMapping
    public ResponseEntity<KnowledgeBaseResponse> create(
            @Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(knowledgeService.create(request));
    }

    @GetMapping("/{id}/documents")
    public Page<DocumentResponse> documents(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return knowledgeService.listDocuments(id,
                PageRequest.of(Math.max(0, page), clamp(size),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<DocumentResponse> addDocument(
            @PathVariable String id,
            @Valid @RequestBody AddDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(knowledgeService.addDocument(id, request));
    }

    /** Retrieval over the tenant's indexed content. */
    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return new SearchResponse(request.query(),
                knowledgeService.search(request.query(), request.topK()));
    }

    private static int clamp(int size) {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }
}

/** Document-level operations, addressed by document id. */
@RestController
@RequestMapping("/knowledge-documents")
@RequiredArgsConstructor
class KnowledgeDocumentController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/{id}/reingest")
    public DocumentResponse reingest(@PathVariable String id) {
        return knowledgeService.reingest(id);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable String id) {
        knowledgeService.deleteDocument(id);
        return new MessageResponse("Document deleted");
    }
}
