package com.vayvora.shared.tenant;

/**
 * Per-request tenant and principal, resolved once by the gateway filter and
 * read by services for the life of the request.
 *
 * <p>Tenant isolation (spec §33) is the requirement that one organization's
 * data can never be read or modified through another organization's session.
 * Every repository query takes an {@code organizationId} argument, and this
 * holder is where that value comes from. It is deliberately not optional: a
 * service that forgets to scope a query fails loudly at
 * {@link #requireOrganizationId()} rather than silently returning another
 * tenant's rows.
 */
public final class TenantContext {

    private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    /** Authenticated caller for the current request. */
    public record Principal(
            String userId,
            String organizationId,
            String email,
            String role) {
    }

    public static void set(Principal principal) {
        CURRENT.set(principal);
    }

    public static Principal get() {
        return CURRENT.get();
    }

    /**
     * Organization for the current request.
     *
     * @throws IllegalStateException if no tenant was resolved, which indicates
     *         a request reached a service without passing the auth filter
     */
    public static String requireOrganizationId() {
        Principal p = CURRENT.get();
        if (p == null || p.organizationId() == null || p.organizationId().isBlank()) {
            throw new IllegalStateException(
                    "No tenant in context; request did not pass authentication");
        }
        return p.organizationId();
    }

    public static String userId() {
        Principal p = CURRENT.get();
        return p == null ? null : p.userId();
    }

    /** Must be called at the end of every request to avoid leaking across pooled threads. */
    public static void clear() {
        CURRENT.remove();
    }
}
