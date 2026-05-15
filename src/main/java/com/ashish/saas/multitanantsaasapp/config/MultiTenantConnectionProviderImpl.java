package com.ashish.saas.multitanantsaasapp.config;

import com.ashish.saas.multitanantsaasapp.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider, HibernatePropertiesCustomizer {
    private final TenantSchemaResolver tenantSchemaResolver;
    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        log.debug("Getting connection for tenant: {}", tenantIdentifier);
        final Connection connection = getAnyConnection();
        try {
            if (tenantIdentifier != null && !tenantIdentifier.equals("public")) {
                connection.createStatement().execute("SET search_path TO" + tenantIdentifier + ", public");
                log.trace("Set search_path to {}", tenantIdentifier);
            }
        } catch (Exception e) {
            log.error("Error setting search_path to {}", tenantIdentifier, e);
            throw new AppException(HttpStatus.BAD_REQUEST, "Error setting search_path to " + tenantIdentifier);
        }
        return connection;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        try {
            connection.createStatement().execute("SET search_path TO public");
        } catch (Exception e) {
            log.error("Error setting search_path to public", e);
            throw new AppException(HttpStatus.BAD_REQUEST,"Error setting search_path to public");
        }

    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public T unwrap(Class<T> unwrapType) {
        return null;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {

    }
}
