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
    private String privateKeyPath;
    private String publicKeyPath;
    private long accessTokenExpiration;

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }
}