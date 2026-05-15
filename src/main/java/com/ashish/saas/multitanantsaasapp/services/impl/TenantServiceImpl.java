package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.TenantRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.TenantResponse;
import com.ashish.saas.multitanantsaasapp.entities.Tenant;
import com.ashish.saas.multitanantsaasapp.entities.TenantStatus;
import com.ashish.saas.multitanantsaasapp.entities.User;
import com.ashish.saas.multitanantsaasapp.entities.UserRole;
import com.ashish.saas.multitanantsaasapp.exception.AppException;
import com.ashish.saas.multitanantsaasapp.mapper.TenantMapper;
import com.ashish.saas.multitanantsaasapp.repositories.TenantRepo;
import com.ashish.saas.multitanantsaasapp.repositories.UserRepo;
import com.ashish.saas.multitanantsaasapp.services.ProvisioningService;
import com.ashish.saas.multitanantsaasapp.services.TenantService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService {

    private final TenantRepo tenantRepo;
    private final TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo;
    private final ProvisioningService provisioningService;

    @Override
    public void registerTenant(TenantRequest request) {

        if (tenantRepo.existsByCompanyCode(request.getCompanyCode())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Tenant already exists"
            );
        }

        if (tenantRepo.existsByEmail(request.getEmail())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Tenant Email already exists"
            );
        }

        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setAdminPassword(
                passwordEncoder.encode(request.getAdminPassword())
        );

        tenant.setStatus(TenantStatus.PENDING);

        tenantRepo.save(tenant);
    }

    @Override
    public void approveTenant(String tenantId) {

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Tenant does not exist"));

        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepo.save(tenant);

        try {
            provisioningService.provisionTenant(tenant);
            createAdminUser(tenant);

        } catch (Exception e) {

            log.error("Tenant provisioning failed : {}", tenantId, e);

            rollbackTenatStatus(tenant);

            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tenant provisioning failed"
            );
        }
    }

    @Override
    public void activateTenant(String tenantId) {

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Tenant does not exist"));
        if (tenant.getStatus() != TenantStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST,"Tenant is not in pending status");
        }
        tenant.setStatus(TenantStatus.ACTIVE);

        tenantRepo.save(tenant);
    }

    @Override
    public void deactivateTenant(String tenantId) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Tenant does not exist"));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AppException(HttpStatus.BAD_REQUEST,"Tenant is not  in active status");
        }
        tenant.setStatus(TenantStatus.INACTIVE);
        tenantRepo.save(tenant);

    }

    @Override
    public void suspendTenant(String tenantId) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Tenant does not exist"));
        if (tenant.getStatus() == TenantStatus.ACTIVE) {
            throw new AppException(HttpStatus.BAD_REQUEST,"Tenant is in active status");
        }
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepo.save(tenant);
    }

    @Override
    public PageResponse<TenantResponse> findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        final Page<Tenant> tenants = tenantRepo.findAll(pageRequest);
        final Page<TenantResponse> pageResponse = tenants.map(tenantMapper::toResponse);
        return PageResponse.of(pageResponse);

    }

    private void createAdminUser(final Tenant tenant) {

        if (userRepo.existsByUsername(
                tenant.getAdminUsername())) {

            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "User already exists"
            );
        }

        User adminUser = User.builder()
                .username(tenant.getAdminUsername())
                .email(tenant.getEmail())
                .firstName(extractFirstName(
                        tenant.getAdminFullName()))
                .lastName(extractLastName(
                        tenant.getAdminFullName()))
                .password(tenant.getAdminPassword())
                .role(UserRole.ROLE_COMPANY_ADMIN)
                .tenant(tenant)
                .deleted(false)
                .build();

        userRepo.save(adminUser);

        log.info("Admin User Created Successfully");
    }

    private String extractFirstName(String adminFullName) {
        String[] names = adminFullName.trim().split("\\s+");
        return names[0];
    }

    private String extractLastName(String adminFullName) {
        String[] names = adminFullName.trim().split("\\s+");

        return names.length > 1
                ? names[names.length - 1]
                : adminFullName;
    }

    private void rollbackTenatStatus(Tenant tenant) {
        tenant.setStatus(TenantStatus.PENDING);
        tenantRepo.save(tenant);
    }
}