package com.ashish.saas.multitanantsaasapp.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP advice that activates the Hibernate {@code tenantFilter} before every service method call.
 *
 * <p>This ensures all JPA queries are automatically scoped to the current tenant, preventing
 * cross-tenant data leakage. If no tenant is present in the context, execution is halted
 * immediately to protect compliance requirements.
 */
@Aspect
@Component
public class TenantHibernateFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantHibernateFilter.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.ashish.saas.multitanantsaasapp.services.impl.*.*(..))")
    public void activateFilter(JoinPoint joinPoint) {
        final String tenantId = TenantContext.getCurrentTenant();

        // Hard fail: never allow a query without a tenant — data leak prevention
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException(
                    "[TENANT VIOLATION] No tenant found in context before calling " +
                    joinPoint.getSignature().toShortString() +
                    ". All service queries require X-Tenant-ID header.");
        }

        log.debug("[TENANT] Activating Hibernate tenantFilter for tenant='{}', method='{}'",
                tenantId, joinPoint.getSignature().toShortString());

        final Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }
}
