package com.vayvora.agentservice.controller;

import com.vayvora.agentservice.dto.CreateAgentRequest;
import com.vayvora.agentservice.dto.AgentResponse;
import com.vayvora.agentservice.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgentController {
    private final AgentService agentService;

    @GetMapping
    public ResponseEntity<?> getAgents(@RequestParam(defaultValue = "50") int limit,
                                       @RequestParam(defaultValue = "0") int offset,
                                       @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            return ResponseEntity.ok(agentService.getAgents(organizationId, limit, offset));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch agents"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createAgent(@RequestBody CreateAgentRequest request,
                                        @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            AgentResponse response = agentService.createAgent(organizationId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to create agent: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAgent(@PathVariable Long id,
                                     @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            AgentResponse response = agentService.getAgent(id, organizationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Agent not found"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAgent(@PathVariable Long id,
                                        @RequestBody CreateAgentRequest request,
                                        @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            AgentResponse response = agentService.updateAgent(id, organizationId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to update agent"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAgent(@PathVariable Long id,
                                        @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            agentService.deleteAgent(id, organizationId);
            return ResponseEntity.ok(new SuccessResponse("Agent deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to delete agent"));
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishAgent(@PathVariable Long id,
                                         @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            AgentResponse response = agentService.publishAgent(id, organizationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to publish agent"));
        }
    }
}

class ErrorResponse {
    private String error;
    public ErrorResponse(String error) { this.error = error; }
    public String getError() { return error; }
}

class SuccessResponse {
    private String message;
    public SuccessResponse(String message) { this.message = message; }
    public String getMessage() { return message; }
}
