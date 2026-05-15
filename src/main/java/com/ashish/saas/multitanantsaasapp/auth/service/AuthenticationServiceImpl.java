package com.ashish.saas.multitanantsaasapp.auth.service;

import com.ashish.saas.multitanantsaasapp.auth.request.LoginRequest;
import com.ashish.saas.multitanantsaasapp.auth.response.LoginResponse;
import com.ashish.saas.multitanantsaasapp.entities.User;
import com.ashish.saas.multitanantsaasapp.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    @Override
    public LoginResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );

        User user = (User) authentication.getPrincipal();

        String token = jwtTokenService.generateAccessToken(
                user.getTenantId(),
                user.getId(),
                user.getRole().name()
        );

        log.info(
                "User authenticated successfully: {}",
                user.getUsername()
        );

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
    }
}