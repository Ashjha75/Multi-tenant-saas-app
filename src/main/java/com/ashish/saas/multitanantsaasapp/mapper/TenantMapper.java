package com.ashish.saas.multitanantsaasapp.mapper;

import com.ashish.saas.multitanantsaasapp.dto.request.TenantRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.TenantResponse;
import com.ashish.saas.multitanantsaasapp.entities.Tenant;
import com.ashish.saas.multitanantsaasapp.entities.TenantStatus;
import org.springframework.stereotype.Service;

@Service
public class TenantMapper {

    public Tenant toEntity(final TenantRequest request) {
        return Tenant.builder()
                .companyName(request.getCompanyName())
                .companyCode(request.getCompanyCode())
                .email(request.getEmail())
                .status(TenantStatus.PENDING) // Default status for new tenants
                .adminFullName(request.getAdminFullName())
                .adminEmail(request.getAdminEmail())
                .adminUsername(request.getAdminUsername())
                .adminPassword(request.getAdminPassword()) // Should be encrypted in service layer
                .deleted(false)
                .build();
    }

    public TenantResponse toResponse(final Tenant entity) {
        return TenantResponse.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .companyCode(entity.getCompanyCode())
                .email(entity.getEmail())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .adminFullName(entity.getAdminFullName())
                .adminEmail(entity.getAdminEmail())
                .adminUsername(entity.getAdminUsername())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}

