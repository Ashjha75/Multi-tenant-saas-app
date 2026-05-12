package com.ashish.saas.multitanantsaasapp.config;

public class TenantContext {
    public static final ThreadLocal<String>  CURRENT_TENANT = new ThreadLocal<>();
    
}
