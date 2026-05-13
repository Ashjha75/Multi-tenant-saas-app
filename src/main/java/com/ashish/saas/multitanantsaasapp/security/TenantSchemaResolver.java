package com.ashish.saas.multitanantsaasapp.security;

import org.springframework.stereotype.Component;

@Component
public class TenantSchemaResolver {

    public String resolveTenantSchema(final String tenantId) {
        // Placeholder until schema-per-tenant is implemented.
        return tenantId;
    }
}

