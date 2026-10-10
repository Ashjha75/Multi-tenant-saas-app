package com.ashish.saas.multitanantsaasapp.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Highest-precedence servlet filter that extracts the {@code X-Tenant-ID} header and
 * stores it in {@link TenantContext} (ThreadLocal) for the duration of the request.
 *
 * <p>Also populates MDC with the tenant ID so every log line emitted during the
 * request automatically includes tenant context — critical for compliance audits.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "app.tenant", name = "mode", havingValue = "filter", matchIfMissing = true)
// enforce header only in filter mode
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    /**
     * Allowed tenant ID format: lowercase alphanumeric, hyphens, underscores, 2–50 chars
     */
    private static final String TENANT_ID_PATTERN = "^[a-z0-9_-]{2,50}$";

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        if (isTenantExempt(path)) {
            chain.doFilter(request, response);
            return;
        }

        final String tenantId = headerResolver(req);

        if (tenantId == null || tenantId.isBlank()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.setContentType("application/json");
            res.getWriter().write("""
                    {
                      "error": "Tenant ID is missing in the request header. Please add the header X-Tenant-ID"
                    }
                    """);
            return;
        }

        // Validate format — prevent path traversal / injection attacks on tenant ID
        if (!tenantId.matches(TENANT_ID_PATTERN)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.setContentType("application/json");
            res.getWriter().write("""
                    {
                      "error": "Invalid X-Tenant-ID format. Must be 2-50 lowercase alphanumeric characters, hyphens or underscores."
                    }
                    """);
            return;
        }

        try {
            TenantContext.setCurrentTenant(tenantId);
            MDC.put("tenantId", tenantId);  // All log lines in this thread will carry tenantId
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
        }
    }

    /**
     * Endpoints that must work without a tenant header. These mirror the permitAll list in
     * {@code SecurityConfig.PUBLIC_URLS}: login happens before a tenant is known, and tenant
     * registration is what creates the tenant in the first place, so requiring X-Tenant-ID on
     * them is a chicken-and-egg deadlock.
     */
    private boolean isTenantExempt(final String path) {
        return path.startsWith("/api/v1/auth/")
                || path.endsWith("/health")
                || path.equals("/error")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v2/api-docs")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/configuration/")
                || path.startsWith("/webjars/")
                || path.equals("/swagger-ui.html");
    }

    private String headerResolver(HttpServletRequest req) {
        final String tenantId = req.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId.toLowerCase();
        }
        return null;
    }
}
