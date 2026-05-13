package com.ashish.saas.multitanantsaasapp.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String privateKeypath;
    private String publicKeyPach;
    private long accessTokenExpiration;
}