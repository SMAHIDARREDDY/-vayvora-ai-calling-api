package com.vayvora.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * Issues and verifies the platform's JWTs.
 *
 * <p>Access tokens carry the organization id, so downstream services can
 * establish tenant context without a database round trip. Refresh tokens are
 * opaque random strings stored hashed (see {@code refresh_tokens}) rather than
 * JWTs, so they can be revoked before expiry.
 */
public class JwtService {

    public static final String CLAIM_ORG = "org";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final long accessTtlMillis;

    /**
     * @param secret         HMAC signing secret; must be at least 32 bytes
     * @param accessTtlMillis access-token lifetime
     */
    public JwtService(String secret, long accessTtlMillis) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 bytes; configure jwt.secret");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtlMillis = accessTtlMillis;
    }

    public String issueAccessToken(String userId, String organizationId,
                                   String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claims(Map.of(
                        CLAIM_ORG, organizationId,
                        CLAIM_ROLE, role,
                        CLAIM_EMAIL, email))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTtlMillis)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies signature and expiry.
     *
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** True when the token is well-formed, correctly signed and unexpired. */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long accessTtlMillis() {
        return accessTtlMillis;
    }

    /**
     * SHA-256 hex digest, used for refresh tokens and API keys.
     *
     * <p>Only the digest is persisted; the raw secret is returned to the caller
     * once at creation and never stored.
     */
    public static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
