package com.ashish.saas.multitanantsaasapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Forces Spring's transaction advisor to be the <em>outermost</em> advice on a bean.
 *
 * <p>{@link TenantHibernateFilter} enables the Hibernate {@code tenantFilter} on the current
 * session before each service call. For that to have any effect it must run <em>inside</em> the
 * transaction, so that {@code entityManager.unwrap(Session.class)} returns the very session the
 * repository query will use.
 *
 * <p>By default both the transaction interceptor and an unordered {@code @Aspect} sit at
 * {@link Ordered#LOWEST_PRECEDENCE}, which leaves their relative order undefined. In practice the
 * tenant aspect ran first — outside the transaction — so it enabled the filter on a short-lived
 * session that was immediately discarded, and every query was emitted with no {@code tenant_id}
 * predicate. That silently returned other tenants' rows.
 *
 * <p>Pinning the transaction advisor to {@link Ordered#HIGHEST_PRECEDENCE} makes it the outer
 * advice, so the tenant aspect always runs within an active transaction.
 */
@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class TransactionAdviceOrderConfig {
}
