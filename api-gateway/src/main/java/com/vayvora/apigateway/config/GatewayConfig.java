package com.vayvora.apigateway.config;

import com.vayvora.shared.security.JwtService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/** Gateway wiring: token verification and browser CORS. */
@Configuration
public class GatewayConfig {

    /**
     * Verifies tokens issued by the auth service.
     *
     * <p>The secret must match {@code jwt.secret} there, or every request will
     * fail signature verification.
     */
    @Bean
    public JwtService jwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:86400000}") long expiration) {
        return new JwtService(secret, expiration);
    }

    /**
     * CORS for the web and Flutter clients.
     *
     * <p>Allowed origins come from configuration rather than a wildcard:
     * credentials are permitted, and browsers reject {@code *} in that case.
     */
    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${cors.allowed-origins:http://localhost:8080,http://localhost:4200}")
            String allowedOrigins) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
