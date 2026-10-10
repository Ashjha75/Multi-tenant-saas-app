package com.ashish.saas.multitanantsaasapp.repositories;

import com.ashish.saas.multitanantsaasapp.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User,String> {
    boolean existsByUsername (String username) ;

    boolean existsByEmail(String email) ;

    Optional<User> findByUsername(String username);

    /**
     * Used by authentication, which runs with the Hibernate "tenantFilter" disabled (see
     * TenantHibernateFilter). That filter normally contributes the "deleted = false" predicate,
     * so it has to be spelled out here or a soft-deleted user could still log in.
     */
    Optional<User> findByUsernameAndDeletedFalse(String username);

    @EntityGraph(attributePaths = "tenant")
    @Query("SELECT U FROM User U WHERE U.id = :id AND U.deleted =false")
    Optional<User> findByIdAndNotDeleted(String id);

    @EntityGraph(attributePaths = "tenant")
    @Query("SELECT U FROM User U WHERE U.tenant.id = :tenantId AND U.deleted = false")
    Page<User> findAllByTenantIdAndNotDeleted(String tenantId, Pageable pageable);
}
