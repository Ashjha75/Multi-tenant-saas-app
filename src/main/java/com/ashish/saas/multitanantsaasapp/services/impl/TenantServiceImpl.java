package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.TenantRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.TenantResponse;
import com.ashish.saas.multitanantsaasapp.entities.Tenant;
import com.ashish.saas.multitanantsaasapp.entities.TenantStatus;
import com.ashish.saas.multitanantsaasapp.exception.AppException;
import com.ashish.saas.multitanantsaasapp.mapper.TenantMapper;
import com.ashish.saas.multitanantsaasapp.repositories.TenantRepo;
import com.ashish.saas.multitanantsaasapp.repositories.UserRepo;
import com.ashish.saas.multitanantsaasapp.services.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void registerTenant(TenantRequest request) {
// check if the tenant already exists by company code
        if (this.tenantRepo.existsByCompanyCode(request.getCompanyCode())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tenant already exists");
        }

// check if email already exists
        if (this.tenantRepo.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tenant Email already exists");
        }

// create tenant entity
        final Tenant tenant = this.tenantMapper.toEntity(request);
        tenant.setAdminPassword(this.passwordEncoder.encode(request.getAdminPassword()));
        tenant.setStatus(TenantStatus.PENDING);
        this.tenantRepo.save(tenant);
    }

    @Override
    public void approveTenant(String tenantId) {

    }

    @Override
    public void activateTenant(String tenantId) {

    }

    @Override
    public void deactivateTenant(String tenantId) {

    }

    @Override
    public void suspendTenant(String tenantId) {

    }

    @Override
    public PageResponse<TenantResponse> findAll(int page, int size) {
        return null;
    }
}
