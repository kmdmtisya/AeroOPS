package com.aeroops.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Binds {@link TenantContext} from the authenticated JWT's tenant claim.
 * Runs after Spring Security's authentication filter (see SecurityConfig), so the
 * Authentication principal is already a validated Jwt by the time this executes.
 * Every downstream repository call is scoped through TenantContext — this is the
 * single choke point the cross-tenant isolation test exercises.
 */
public class TenantFilter extends OncePerRequestFilter {

    private final String tenantClaim;

    public TenantFilter(String tenantClaim) {
        this.tenantClaim = tenantClaim;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                String tenantId = jwt.getClaimAsString(tenantClaim);
                if (tenantId == null || tenantId.isBlank()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token is missing tenant claim");
                    return;
                }
                TenantContext.set(tenantId);
                String actor = jwt.getClaimAsString("preferred_username");
                if (actor != null && !actor.isBlank()) {
                    ActorContext.set(actor);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            ActorContext.clear();
        }
    }
}
