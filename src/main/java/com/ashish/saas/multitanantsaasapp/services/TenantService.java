package com.ashish.saas.multitanantsaasapp.services;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.TenantRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.TenantResponse;

public interface TenantService {
    void registerTenant(final TenantRequest request);
    void approveTenant(final String tenantId);  void activateTenant(final String tenantId); 

    void deactivateTenant(final String tenantId); 

    void suspendTenant(final String tenantId); 

    PageResponse<TenantResponse> findAll(final int page, final int size) ;
}
