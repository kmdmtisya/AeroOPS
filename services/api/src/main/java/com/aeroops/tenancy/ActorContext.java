package com.aeroops.tenancy;

/**
 * Request-scoped holder for the authenticated caller's identity ("who," not "which tenant" —
 * see {@link TenantContext}). Set exactly once per request by {@link TenantFilter} from the
 * JWT's preferred_username claim, consumed by audit-writing code (AuditEvent.actor).
 */
public final class ActorContext {

    private static final ThreadLocal<String> CURRENT_ACTOR = new ThreadLocal<>();

    private ActorContext() {
    }

    public static void set(String actor) {
        CURRENT_ACTOR.set(actor);
    }

    public static String get() {
        String actor = CURRENT_ACTOR.get();
        if (actor == null) {
            throw new IllegalStateException("No actor bound to the current request");
        }
        return actor;
    }

    public static void clear() {
        CURRENT_ACTOR.remove();
    }
}
