package com.ashish.saas.multitanantsaasapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaResolver {

    private static final String PUBLIC_SCHEMA = "public";

    private final JdbcTemplate jdbcTemplate;

    @Cacheable(
            cacheNames = "tenantSchemas",
            key = "#tenantId"
    )
    public String resolveTenantSchema(final String tenantId) {

        if (tenantId == null) {
            return PUBLIC_SCHEMA;
        }

        try {

            String companyCode =
                    jdbcTemplate.queryForObject(
                            """
                                    SELECT company_code
                                    FROM public.tenants
                                    WHERE id = ?
                                    AND deleted = false
                                    """,
                            String.class,
                            tenantId
                    );

            if (companyCode != null) {

                String schemaName =
                        "tenant_" +
                                companyCode
                                        .trim()
                                        .toLowerCase()
                                        .replaceAll("[^a-z0-9_]", "_");

                log.debug(
                        "Resolved tenantId: {} to schema: {}",
                        tenantId,
                        schemaName
                );

                return schemaName;
            }

            log.warn(
                    "Schema not found for tenant: {}. Using public schema",
                    tenantId
            );

        } catch (Exception e) {

            log.error(
                    "Error resolving schema for tenantId: {}",
                    tenantId,
                    e
            );
        }

        return PUBLIC_SCHEMA;
    }
}