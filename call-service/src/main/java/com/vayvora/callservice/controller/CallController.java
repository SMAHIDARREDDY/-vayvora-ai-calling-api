package com.vayvora.callservice.controller;

import com.vayvora.callservice.dto.CallDtos.AppendMessageRequest;
import com.vayvora.callservice.dto.CallDtos.CallDetailResponse;
import com.vayvora.callservice.dto.CallDtos.CallIntelligenceRequest;
import com.vayvora.callservice.dto.CallDtos.CallResponse;
import com.vayvora.callservice.dto.CallDtos.InitiateCallRequest;
import com.vayvora.callservice.dto.CallDtos.MessageResponse;
import com.vayvora.callservice.dto.CallDtos.TranscriptResponse;
import com.vayvora.callservice.dto.CallDtos.UpdateCallStateRequest;
import com.vayvora.callservice.service.CallService;
import com.vayvora.shared.enums.Enums.CallState;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Call endpoints (spec §30). */
@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
public class CallController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CallService callService;

    @GetMapping
    public Page<CallResponse> list(
            @RequestParam(required = false) CallState state,
            @RequestParam(required = false) String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return callService.list(state, agentId,
                PageRequest.of(Math.max(0, page), clamp(size),
                        Sort.by(Sort.Direction.DESC, "startedAt")));
    }

    /** Declared before {@code /{id}} so "live" is not parsed as a call id. */
    @GetMapping("/live")
    public List<CallResponse> live() {
        return callService.live();
    }

    @GetMapping("/{id}")
    public CallResponse get(@PathVariable String id) {
        return callService.get(id);
    }

    /** Call plus every artifact derived from it. */
    @GetMapping("/{id}/detail")
    public CallDetailResponse detail(@PathVariable String id) {
        return callService.detail(id);
    }

    @GetMapping("/{id}/transcript")
    public TranscriptResponse transcript(@PathVariable String id) {
        return callService.transcript(id);
    }

    @PostMapping
    public ResponseEntity<CallResponse> initiate(
            @Valid @RequestBody InitiateCallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(callService.initiate(request));
    }

    @PatchMapping("/{id}/state")
    public CallResponse updateState(@PathVariable String id,
                                    @Valid @RequestBody UpdateCallStateRequest request) {
        return callService.updateState(id, request);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> appendMessage(
            @PathVariable String id,
            @Valid @RequestBody AppendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(callService.appendMessage(id, request));
    }

    /** Attaches summary, sentiment, intent and lead score after the call. */
    @PostMapping("/{id}/intelligence")
    public CallDetailResponse attachIntelligence(
            @PathVariable String id,
            @RequestBody CallIntelligenceRequest request) {
        return callService.attachIntelligence(id, request);
    }

    private static int clamp(int size) {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }
}
