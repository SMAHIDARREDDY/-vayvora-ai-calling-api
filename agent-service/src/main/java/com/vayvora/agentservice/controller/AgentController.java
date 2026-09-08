package com.vayvora.agentservice.controller;

import com.vayvora.agentservice.dto.AgentDtos.AgentConfigRequest;
import com.vayvora.agentservice.dto.AgentDtos.AgentResponse;
import com.vayvora.agentservice.dto.AgentDtos.AgentVersionResponse;
import com.vayvora.agentservice.dto.AgentDtos.ToolResponse;
import com.vayvora.agentservice.dto.AgentDtos.VoiceResponse;
import com.vayvora.agentservice.service.AgentService;
import com.vayvora.shared.enums.Enums.AgentStatus;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AI agent endpoints (spec §30). */
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AgentService agentService;

    @GetMapping
    public Page<AgentResponse> list(
            @RequestParam(required = false) AgentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return agentService.list(status,
                PageRequest.of(Math.max(0, page), clamp(size),
                        Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    /** Voices and tools are listed before the id route so they are not read as ids. */
    @GetMapping("/voices")
    public List<VoiceResponse> voices() {
        return agentService.availableVoices();
    }

    @GetMapping("/tools")
    public List<ToolResponse> tools() {
        return agentService.availableTools();
    }

    @GetMapping("/{id}")
    public AgentResponse get(@PathVariable String id) {
        return agentService.get(id);
    }

    @GetMapping("/{id}/versions")
    public List<AgentVersionResponse> versions(@PathVariable String id) {
        return agentService.versionHistory(id);
    }

    @PostMapping
    public ResponseEntity<AgentResponse> create(
            @Valid @RequestBody AgentConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentService.create(request));
    }

    @PutMapping("/{id}")
    public AgentResponse update(@PathVariable String id,
                                @Valid @RequestBody AgentConfigRequest request) {
        return agentService.update(id, request);
    }

    @PostMapping("/{id}/publish")
    public AgentResponse publish(@PathVariable String id) {
        return agentService.publish(id);
    }

    @PostMapping("/{id}/archive")
    public AgentResponse archive(@PathVariable String id) {
        return agentService.archive(id);
    }

    private static int clamp(int size) {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }
}
