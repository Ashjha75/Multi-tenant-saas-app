package com.ashish.saas.multitanantsaasapp.mapper;

import com.ashish.saas.multitanantsaasapp.config.TenantContext;
import com.ashish.saas.multitanantsaasapp.dto.request.UserRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.UserResponse;
import com.ashish.saas.multitanantsaasapp.entities.Tenant;
import com.ashish.saas.multitanantsaasapp.entities.User;
import com.ashish.saas.multitanantsaasapp.entities.UserRole;
import org.springframework.stereotype.Service;

@Service
public class UserMapper {

    public User toEntity(final UserRequest request, final Tenant tenant) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword()) // Should be encrypted in service layer
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.valueOf(request.getRole()))
                .tenant(tenant)
                .enabled(true) // Default: new users are enabled
                .deleted(false)
                .build();
    }

    public UserResponse toResponse(final User entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .role(entity.getRole() != null ? entity.getRole().name() : null)
                .enabled(entity.isEnabled())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .tenantCompanyName(entity.getTenant() != null ? entity.getTenant().getCompanyName() : null)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}

