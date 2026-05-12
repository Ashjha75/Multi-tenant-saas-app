package com.ashish.saas.multitanantsaasapp.repositories;

import com.ashish.saas.multitanantsaasapp.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepo extends JpaRepository<Category, String> {

    /**
     * Tenant-scoped, case-insensitive name lookup.
     * Uses explicit JPQL to guarantee tenant isolation — avoids relying solely on
     * session-level Hibernate filter which may not apply in all execution paths.
     */
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.tenantId = :tenantId AND c.deleted = false")
    Optional<Category> findByNameIgnoreCaseAndTenant(@Param("name") String name, @Param("tenantId") String tenantId);
}
