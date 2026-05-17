package com.ashish.saas.multitanantsaasapp.security;

import com.ashish.saas.multitanantsaasapp.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final ResourceLoader resourceLoader;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            log.info("Loading JWT keys...");
            log.info("Private path: {}", jwtProperties.getPrivateKeyPath());
            log.info("Public path: {}", jwtProperties.getPublicKeyPath());

            this.privateKey = loadPrivateKey(
                    jwtProperties.getPrivateKeyPath());

            this.publicKey = loadPublicKey(
                    jwtProperties.getPublicKeyPath());

            log.info("JWT keys loaded successfully");

        } catch (Exception e) {
            log.error("Failed to load JWT keys", e);
            throw new IllegalStateException(
                    "Unable to initialize JWT keys", e);
        }
    }

    public String generateAccessToken(
            @Nonnull String tenantId,
            @Nonnull String userId,
            String role) {

        Date now = new Date();

        Date expiration = new Date(
                System.currentTimeMillis()
                        + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId)
                .claim("tenant_id", tenantId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .issuer("stock-saas-app")
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public boolean validateToken(String token) {

        try {
            Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("Token expired");

        } catch (MalformedJwtException e) {
            throw new UnauthorizedException("Malformed token");

        } catch (UnsupportedJwtException e) {
            throw new UnauthorizedException("Unsupported token");

        } catch (SecurityException e) {
            throw new UnauthorizedException("Invalid signature");

        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Empty token");
        }
    }

    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getTenantIdFromToken(String token) {
        return getClaims(token)
                .get("tenant_id", String.class);
    }

    public String getRoleFromToken(String token) {
        return getClaims(token)
                .get("role", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PrivateKey loadPrivateKey(String path)
            throws Exception {

        String pem = readPem(path)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded =
                Base64.getDecoder().decode(pem);

        return KeyFactory.getInstance("RSA")
                .generatePrivate(
                        new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey(String path)
            throws Exception {

        String pem = readPem(path)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded =
                Base64.getDecoder().decode(pem);

        return KeyFactory.getInstance("RSA")
                .generatePublic(
                        new X509EncodedKeySpec(decoded));
    }

    private String readPem(String path)
            throws Exception {

        Resource resource =
                resourceLoader.getResource(path);

        if (!resource.exists()) {
            throw new RuntimeException(
                    "JWT key not found: " + path);
        }

        try (InputStream is =
                     resource.getInputStream()) {

            return new String(
                    is.readAllBytes(),
                    StandardCharsets.UTF_8);
        }
    }
}