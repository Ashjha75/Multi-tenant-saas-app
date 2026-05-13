package com.ashish.saas.multitanantsaasapp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "User details returned in API responses")
public class UserResponse {

    @Schema(description = "Unique identifier (UUID) of the user", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(description = "Unique username for login", example = "john.doe")
    private String username;

    @Schema(description = "User email address", example = "john.doe@acme.com")
    private String email;

    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Schema(description = "User role (e.g., ADMIN, USER, MANAGER)", example = "USER")
    private String role;

    @Schema(description = "Whether the user account is enabled", example = "true")
    private boolean enabled;

    @Schema(description = "Tenant company name that this user belongs to", example = "Acme Corporation")
    private String tenantCompanyName;

    @Schema(description = "Tenant ID that this user belongs to", example = "alpha")
    private String tenantId;

    @Schema(description = "Timestamp when the user was created")
    private LocalDateTime createdAt;

    @Schema(description = "User who created this user record")
    private String createdBy;

    @Schema(description = "Timestamp when the user record was last modified")
    private LocalDateTime updatedAt;

    @Schema(description = "User who last modified this user record")
    private String updatedBy;
}

