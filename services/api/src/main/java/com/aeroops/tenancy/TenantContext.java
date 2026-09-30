package com.aeroops.tenancy;

/**
 * Request-scoped holder for the authenticated caller's tenant.
 * Set exactly once per request by {@link TenantFilter} from the JWT claim —
 * never accept a tenant id from a request parameter or path segment.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String get() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant bound to the current request");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static boolean isBound() {
        return CURRENT_TENANT.get() != null;
    }
}
