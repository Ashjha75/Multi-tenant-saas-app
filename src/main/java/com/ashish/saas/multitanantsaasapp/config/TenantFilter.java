package com.ashish.saas.multitanantsaasapp.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;

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

        // Optional: store tenant in ThreadLocal/context here
        try {
            TenantContext.setCurrentTenant(tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String headerResolver(HttpServletRequest req) {
        final String tenantId = req.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId.toLowerCase();
        }

        return null;
    }
}