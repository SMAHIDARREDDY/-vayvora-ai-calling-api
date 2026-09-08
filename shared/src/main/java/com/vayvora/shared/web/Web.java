package com.vayvora.shared.web;

import com.vayvora.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cross-cutting web plumbing: identity headers, error envelope, exception
 * translation and the filter that establishes tenant context.
 */
public final class Web {

    private Web() {
    }

    /**
     * Headers the gateway sets after validating a token.
     *
     * <p>Downstream services trust these because they are only reachable
     * through the gateway, which strips any client-supplied copies.
     */
    public static final class Headers {
        public static final String USER_ID = "X-Vayvora-User-Id";
        public static final String ORG_ID = "X-Vayvora-Org-Id";
        public static final String EMAIL = "X-Vayvora-Email";
        public static final String ROLE = "X-Vayvora-Role";

        private Headers() {
        }
    }

    /** Uniform error body returned by every service. */
    public record ApiError(
            String error,
            String message,
            int status,
            Instant timestamp,
            List<String> details) {

        public static ApiError of(HttpStatus status, String message) {
            return new ApiError(status.getReasonPhrase(), message,
                    status.value(), Instant.now(), List.of());
        }

        public static ApiError of(HttpStatus status, String message, List<String> details) {
            return new ApiError(status.getReasonPhrase(), message,
                    status.value(), Instant.now(), details);
        }
    }

    /** Requested entity does not exist, or is not visible to this tenant. */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** Request is well-formed but not permitted in the current state. */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    /** Caller is authenticated but lacks the required permission. */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    /** Request failed validation beyond what bean validation covers. */
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    /**
     * Translates exceptions into the shared error envelope.
     *
     * <p>Registered in each service via component scan of this package.
     */
    @RestControllerAdvice
    public static class ApiExceptionHandler {

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<ApiError> notFound(NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(HttpStatus.NOT_FOUND, e.getMessage()));
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ApiError> conflict(ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of(HttpStatus.CONFLICT, e.getMessage()));
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ApiError> forbidden(ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.of(HttpStatus.FORBIDDEN, e.getMessage()));
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiError> unauthorized(UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiError.of(HttpStatus.UNAUTHORIZED, e.getMessage()));
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiError> badRequest(BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.of(HttpStatus.BAD_REQUEST, e.getMessage()));
        }

        /** Bean-validation failures, reported field by field. */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
            List<String> details = e.getBindingResult().getFieldErrors().stream()
                    .map(f -> f.getField() + ": " + f.getDefaultMessage())
                    .toList();
            return ResponseEntity.badRequest()
                    .body(ApiError.of(HttpStatus.BAD_REQUEST, "Validation failed", details));
        }

        /**
         * A request that reached a service without tenant context.
         *
         * <p>Reported as 401 rather than 500: the cause is always a missing or
         * unvalidated identity, not a server fault.
         */
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ApiError> illegalState(IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("No tenant in context")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiError.of(HttpStatus.UNAUTHORIZED, "Authentication required"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiError.of(HttpStatus.BAD_REQUEST, e.getMessage()));
        }
    }

    /**
     * Populates {@link TenantContext} from the gateway's identity headers.
     *
     * <p>Always clears the context in a finally block: the servlet container
     * reuses threads, and a stale principal would attribute one tenant's
     * request to another.
     */
    public static class TenantFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain)
                throws ServletException, IOException {
            try {
                String orgId = request.getHeader(Headers.ORG_ID);
                if (orgId != null && !orgId.isBlank()) {
                    TenantContext.set(new TenantContext.Principal(
                            request.getHeader(Headers.USER_ID),
                            orgId,
                            request.getHeader(Headers.EMAIL),
                            request.getHeader(Headers.ROLE)));
                }
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
