package com.vayvora.apigateway.filter;

import com.vayvora.shared.security.JwtService;
import com.vayvora.shared.web.Web;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates the bearer token and forwards the caller's identity downstream.
 *
 * <p>Client-supplied identity headers are stripped unconditionally before the
 * validated ones are attached. Without that, a caller could set
 * {@code X-Vayvora-Org-Id} by hand and read another tenant's data, since
 * services trust those headers by design (spec §33).
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    /** Paths reachable without a token. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/actuator/health");

    private final JwtService jwtService;

    public AuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublic(path)) {
            // Strip identity headers even on public routes: a caller must not
            // be able to pre-set them and have a later hop trust the values.
            return chain.filter(exchange.mutate()
                    .request(stripIdentityHeaders(request).build())
                    .build());
        }

        String token = bearerToken(request);
        if (token == null) {
            return reject(exchange, "Authentication required");
        }

        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (Exception e) {
            return reject(exchange, "Invalid or expired token");
        }

        String organizationId = claims.get(JwtService.CLAIM_ORG, String.class);
        if (organizationId == null || organizationId.isBlank()) {
            return reject(exchange, "Token is missing an organization claim");
        }

        ServerHttpRequest mutated = stripIdentityHeaders(request)
                .header(Web.Headers.USER_ID, claims.getSubject())
                .header(Web.Headers.ORG_ID, organizationId)
                .header(Web.Headers.EMAIL,
                        nullSafe(claims.get(JwtService.CLAIM_EMAIL, String.class)))
                .header(Web.Headers.ROLE,
                        nullSafe(claims.get(JwtService.CLAIM_ROLE, String.class)))
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /**
     * Runs before routing so an unauthenticated request never reaches a
     * downstream service.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private static ServerHttpRequest.Builder stripIdentityHeaders(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            headers.remove(Web.Headers.USER_ID);
            headers.remove(Web.Headers.ORG_ID);
            headers.remove(Web.Headers.EMAIL);
            headers.remove(Web.Headers.ROLE);
        });
    }

    private static String bearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            return null;
        }
        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /** Writes the shared error envelope without invoking a downstream service. */
    private static Mono<Void> reject(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message
                + "\",\"status\":401}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
