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

    @Override
    public PageResponse<TenantResponse> findPending(int page, int size) {
        final PageRequest pageRequest = PageRequest.of(page, size);
        final Page<Tenant> tenants = tenantRepo.findAllByStatus(TenantStatus.PENDING, pageRequest);
        final Page<TenantResponse> pageResponse = tenants.map(tenantMapper::toResponse);
        return PageResponse.of(pageResponse);
    }

    private void createAdminUser(final Tenant tenant) {

        // Check by username — if admin already belongs to this tenant, skip
        if (userRepo.existsByUsername(tenant.getAdminUsername())) {
            final User existing = userRepo.findByUsername(tenant.getAdminUsername())
                    .orElse(null);
            if (existing != null && isSameTenant(existing, tenant)) {
                log.info("Admin user already exists for tenant: {}", tenant.getId());
                return;
            }
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Username '" + tenant.getAdminUsername() + "' is already taken"
            );
        }

        // Check by email — same tenant-ownership logic
        if (userRepo.existsByEmail(tenant.getAdminEmail())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Email '" + tenant.getAdminEmail() + "' is already taken"
            );
        }

        User adminUser = User.builder()
                .username(tenant.getAdminUsername())
                .email(tenant.getAdminEmail())
                .firstName(extractFirstName(
                        tenant.getAdminFullName()))
                .lastName(extractLastName(
                        tenant.getAdminFullName()))
                .password(tenant.getAdminPassword())
                .role(UserRole.ROLE_COMPANY_ADMIN)
                .tenantId(tenant.getId())
                .tenant(tenant)
                .enabled(true)
                .deleted(false)
                .build();

        userRepo.save(adminUser);

        log.info("Admin User Created Successfully for tenant: {}", tenant.getCompanyCode());
    }

    /**
     * Check if the user belongs to the given tenant.
     * Compares both the ManyToOne FK (getTenant()) and the AbstractEntity tenantId field.
     */
    private boolean isSameTenant(User user, Tenant tenant) {
        // Check the ManyToOne FK relationship
        if (user.getTenant() != null) {
            return tenant.getId().equals(user.getTenant().getId());
        }
        // Fallback: compare the AbstractEntity tenantId field (set by @PrePersist / seed data)
        if (user.getTenantId() != null) {
            return tenant.getId().equals(user.getTenantId());
        }
        return false;
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
