package com.ashish.saas.multitanantsaasapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
// Schema routing only. In "filter" mode isolation is done by the tenant_id column, and
// registering a MultiTenantConnectionProvider would switch Hibernate into multi-tenant
// mode for no reason. See docs/tenant-toggle.md.
@ConditionalOnProperty(prefix = "app.tenant", name = "mode", havingValue = "schema")
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver, HibernatePropertiesCustomizer {
    @Override
    public String resolveCurrentTenantIdentifier() {
        final String schema = TenantContext.getCurrentSchema();
        log.trace("Current tenant schema: {}", schema);
        if (schema == null) {
            return "public";
        }
        return schema;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        // This is the identifier resolver, not the connection provider. Registering it under
        // MULTI_TENANT_CONNECTION_PROVIDER overwrote the real provider (or was overwritten by
        // it, depending on bean order) and left Hibernate with no tenant resolver at all.
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}