package com.ashish.saas.multitanantsaasapp.repositories;

import com.ashish.saas.multitanantsaasapp.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User,String> {
    boolean existsByUsername (String username) ;

    boolean existsByEmail(String email) ;

    Optional<User> findByUsername(String username);

    @Query("SELECT U FROM User U WHERE U.id = :id AND U.deleted =false")
    Optional<User> findByIdAndNotDeleted(String id);

    @Query("SELECT U FROM User U WHERE U.tenant.id = :tenantId AND U.deleted = false")
    Page<User> findAllByTenantIdAndNotDeleted(String tenantId, Pageable pageable);
}
