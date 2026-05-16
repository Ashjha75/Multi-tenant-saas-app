package com.ashish.saas.multitanantsaasapp.auth;

import com.ashish.saas.multitanantsaasapp.auth.request.LoginRequest;
import com.ashish.saas.multitanantsaasapp.auth.response.LoginResponse;
import com.ashish.saas.multitanantsaasapp.auth.service.AuthenticationService;
import com.ashish.saas.multitanantsaasapp.dto.request.TenantRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.TenantResponse;
import com.ashish.saas.multitanantsaasapp.services.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication APIs",
        description = "Authentication and tenant registration APIs"
)
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final TenantService tenantService;

    @Operation(
            summary = "Login user",
            description = "Authenticate a user and generate JWT access token"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            schema = @Schema(
                                    implementation = LoginResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody final LoginRequest request) {

        LoginResponse response =
                authenticationService.login(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Register tenant",
            description = "Register a new company tenant"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tenant registered successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Tenant already exists"
            )
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody final TenantRequest request) {

        tenantService.registerTenant(request);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Logout user",
            description = "Client-side logout endpoint for clearing stored tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful"
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}