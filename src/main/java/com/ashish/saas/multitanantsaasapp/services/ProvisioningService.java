package com.ashish.saas.multitanantsaasapp.services;
import com.ashish.saas.multitanantsaasapp.entities.Tenant;

public interface ProvisioningService {

    void provisionTenant(final Tenant tenant);
}