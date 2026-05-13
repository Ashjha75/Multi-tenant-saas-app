package com.ashish.saas.multitanantsaasapp.security;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {
    private final JwtProperties jwtProperties;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            this.privateKey = loadPrivateKey(this.jwtProperties.getPrivateKeyPath());
            this.publicKey = loadPublicKey(this.jwtProperties.getPublicKeyPath());
            log.info("Private & Public key Loaded successfully");
        } catch (final Exception e) {
            log.error("Error loading private key", e);
            throw new RuntimeException("Error loading private key", e);
        }
    }

    public String generateAccessToken(
            @Nonnull final String tenantId,
            @Nonnull final String userId, final String role) {
        final Date now = new Date();
        final Date expiration = new Date(System.currentTimeMillis() + this.jwtProperties.getAccessTokenExpiration());

        return Jwts.builder().
                subject(userId).
                claim("tenant_id", tenantId).
                claim("role", role).
                issuedAt(now).
                expiration(expiration).
                issuer("stock-saas-app").
                signWith(this.privateKey, Jwts.SIG.RS256)
                .compact();

    }
