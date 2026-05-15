package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.entities.Tenant;
import com.ashish.saas.multitanantsaasapp.exception.AppException;
import com.ashish.saas.multitanantsaasapp.services.ProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisioningServiceImpl implements ProvisioningService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void provisionTenant(final Tenant tenant) {
        final String schemaName = buildSchemaName(
                tenant.getCompanyCode());

        try {

            log.info(
                    "Provisioning tenant: {} with schema: {}",
                    tenant.getCompanyName(),
                    schemaName
            );

            // 1. Create schema
            createSchema(schemaName);

            // 2. Run Flyway migrations
            runTenantMigrations(schemaName);

            // 3. Seed default data (optional)
            initializeDefaultData(schemaName, tenant);

            log.info(
                    "Tenant {} provisioned successfully",
                    tenant.getCompanyName()
            );

        } catch (Exception e) {

            log.error(
                    "Error provisioning tenant: {}",
                    tenant.getCompanyName(),
                    e
            );
            try {
                dropSchema(schemaName);
            } catch (Exception ex) {
                log.error(
                        "Error dropping schema: {}",
                        schemaName,
                        ex
                );
            }
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Failed to provision tenant"
            );
        }
    }

    private void createSchema(final String schemaName) {

        final String sql = String.format(
                "CREATE SCHEMA IF NOT EXISTS \"%s\"",
                schemaName
        );

        jdbcTemplate.execute(sql);
        log.info("Schema created: {}", schemaName);
    }

    private void dropSchema(final String schemaName) {

        final String sql = String.format(
                "DROP SCHEMA IF EXISTS \"%s\" CASCADE",
                schemaName
        );

        jdbcTemplate.execute(sql);

        log.info("Schema dropped: {}", schemaName);
    }

    private void runTenantMigrations(final String schemaName) {
        log.info(
                "Running Flyway migrations for schema: {}",
                schemaName
        );
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .table("flyway_schema_history")
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .load();

        flyway.migrate();

        log.info(
                "Flyway migrations completed for schema: {}",
                schemaName
        );
    }

    private void initializeDefaultData(
            final String schemaName,
            final Tenant tenant
    ) {

        // Optional seed data
    }

    private String buildSchemaName(String companyCode) {

        return "tenant_" +
                companyCode
                        .trim()
                        .toLowerCase()
                        .replaceAll("[^a-z0-9_]", "_");
    }
}