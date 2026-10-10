package com.ashish.saas.multitanantsaasapp.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

/**
 * AOP advice that activates the Hibernate {@code tenantFilter} before every service method call.
 *
 * <p>This ensures all JPA queries are automatically scoped to the current tenant, preventing
 * cross-tenant data leakage. If no tenant is present in the context, execution is halted
 * immediately to protect compliance requirements.
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "app.tenant", name = "mode", havingValue = "filter", matchIfMissing = true)
// Innermost advice: must run inside the transaction opened by the transaction advisor,
// which TransactionAdviceOrderConfig pins to HIGHEST_PRECEDENCE.
@Order(Ordered.LOWEST_PRECEDENCE)
// toggle: enable only for filter mode
public class TenantHibernateFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantHibernateFilter.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Service methods that legitimately run before any tenant is known.
     *
     * <p>{@code loadUserByUsername} is Spring Security's {@code UserDetailsService} entry point,
     * invoked during {@code POST /api/v1/auth/login} — i.e. before the caller has a token and
     * therefore before a tenant can be established. Requiring a tenant here would make login
     * impossible. The lookup is safe without the filter because {@code users.username} carries a
     * global UNIQUE constraint (uc_users_username), so it can resolve to at most one row, and the
     * tenant is then taken from that row to build the JWT.
     */
    private static final Set<String> TENANT_EXEMPT_METHODS = Set.of(
            "loadUserByUsername",
            // POST /api/v1/auth/register is public and is what creates a tenant, so the caller
            // cannot already have one. It only writes the Tenant row, whose tenant_id is derived
            // from its own company_code in AbstractEntity#onCreate.
            "registerTenant",
            // Raw DDL + Flyway on their own JDBC connections; no Hibernate session is involved,
            // and it deliberately runs with the caller's transaction suspended.
            "provisionTenant");

    @Before("execution(* com.ashish.saas.multitanantsaasapp.services.impl.*.*(..))")
    public void activateFilter(JoinPoint joinPoint) {
        final String tenantId = TenantContext.getCurrentTenant();

        if (TENANT_EXEMPT_METHODS.contains(joinPoint.getSignature().getName())) {
            log.debug("[TENANT] Skipping tenantFilter for pre-authentication method '{}'",
                    joinPoint.getSignature().toShortString());
            return;
        }

        // Hard fail: never allow a query without a tenant — data leak prevention
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException(
                    "[TENANT VIOLATION] No tenant found in context before calling " +
                            joinPoint.getSignature().toShortString() +
                            ". All service queries require X-Tenant-ID header.");
        }

        log.debug("[TENANT] Activating Hibernate tenantFilter for tenant='{}', method='{}'",
                tenantId, joinPoint.getSignature().toShortString());

        // Enabling the filter only affects the session bound to the current transaction. With no
        // active transaction, unwrap() hands back a throwaway session that is discarded straight
        // away, the tenant_id predicate never reaches the query, and the call silently returns
        // every tenant's rows. Fail loudly instead of leaking.
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "[TENANT VIOLATION] No active transaction while calling " +
                            joinPoint.getSignature().toShortString() +
                            ". The Hibernate tenantFilter cannot be applied, which would expose " +
                            "other tenants' rows. Annotate the service with @Transactional.");
        }

        final Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }
}
