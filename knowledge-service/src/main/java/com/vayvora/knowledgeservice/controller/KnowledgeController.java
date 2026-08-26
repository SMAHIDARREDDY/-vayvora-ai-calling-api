package com.vayvora.knowledgeservice.controller;

import com.vayvora.knowledgeservice.dto.*;
import com.vayvora.knowledgeservice.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    @PostMapping
    public ResponseEntity<?> createKnowledgeBase(@RequestBody CreateKnowledgeBaseRequest request,
                                                @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            KnowledgeBaseResponse response = knowledgeService.createKnowledgeBase(organizationId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to create knowledge base"));
        }
    }

    @PostMapping("/{knowledgeBaseId}/documents")
    public ResponseEntity<?> addDocument(@PathVariable Long knowledgeBaseId,
                                        @RequestBody AddDocumentRequest request,
                                        @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            DocumentResponse response = knowledgeService.addDocument(knowledgeBaseId, organizationId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to add document"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchKnowledge(@RequestParam String query,
                                            @RequestParam(required = false) Long organizationId) {
        try {
            return ResponseEntity.ok(knowledgeService.search(query, organizationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to search knowledge"));
        }
    }

    @GetMapping("/{knowledgeBaseId}/documents")
    public ResponseEntity<?> getDocuments(@PathVariable Long knowledgeBaseId,
                                         @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            return ResponseEntity.ok(knowledgeService.getDocuments(knowledgeBaseId, organizationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch documents"));
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long documentId,
                                           @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            knowledgeService.deleteDocument(documentId, organizationId);
            return ResponseEntity.ok(new SuccessResponse("Document deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to delete document"));
        }
    }
}

class ErrorResponse {
    private String error;
    public ErrorResponse(String error) { this.error = error; }
}

class SuccessResponse {
    private String message;
    public SuccessResponse(String message) { this.message = message; }
}
