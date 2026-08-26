package com.vayvora.callservice.controller;

import com.vayvora.callservice.dto.InitiateCallRequest;
import com.vayvora.callservice.dto.CallResponse;
import com.vayvora.callservice.dto.UpdateCallStatusRequest;
import com.vayvora.callservice.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calls")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CallController {
    private final CallService callService;

    @PostMapping
    public ResponseEntity<?> initiateCall(@RequestBody InitiateCallRequest request) {
        try {
            CallResponse response = callService.initiateCall(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to initiate call: " + e.getMessage()));
        }
    }

    @GetMapping("/{callId}")
    public ResponseEntity<?> getCall(@PathVariable String callId,
                                    @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            CallResponse response = callService.getCall(callId, organizationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Call not found"));
        }
    }

    @PatchMapping("/{callId}/status")
    public ResponseEntity<?> updateCallStatus(@PathVariable String callId,
                                             @RequestBody UpdateCallStatusRequest request,
                                             @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            CallResponse response = callService.updateCallStatus(callId, organizationId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to update call status"));
        }
    }

    @GetMapping("/organizations/{organizationId}/calls")
    public ResponseEntity<?> getOrganizationCalls(@PathVariable Long organizationId,
                                                 @RequestParam(defaultValue = "50") int limit,
                                                 @RequestParam(defaultValue = "0") int offset,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Long agentId) {
        try {
            return ResponseEntity.ok(callService.getOrganizationCalls(organizationId, limit, offset, status, agentId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch calls"));
        }
    }

    @PostMapping("/{callId}/recording")
    public ResponseEntity<?> uploadRecording(@PathVariable String callId,
                                            @RequestBody RecordingUploadRequest request,
                                            @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            CallResponse response = callService.uploadRecording(callId, organizationId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to upload recording"));
        }
    }

    @DeleteMapping("/{callId}")
    public ResponseEntity<?> deleteCall(@PathVariable String callId,
                                       @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            callService.deleteCall(callId, organizationId);
            return ResponseEntity.ok(new SuccessResponse("Call deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to delete call"));
        }
    }
}

class RecordingUploadRequest {
    private String recordingUrl;
    private Integer duration;
    
    public String getRecordingUrl() { return recordingUrl; }
    public Integer getDuration() { return duration; }
}

class ErrorResponse {
    private String error;
    public ErrorResponse(String error) { this.error = error; }
}

class SuccessResponse {
    private String message;
    public SuccessResponse(String message) { this.message = message; }
}
