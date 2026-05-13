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
@Schema(description = "Tenant details returned in API responses")
public class TenantResponse {

    @Schema(description = "Unique identifier (UUID) of the tenant", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(description = "Company display name", example = "Acme Corporation")
    private String companyName;

    @Schema(description = "Unique company identifier", example = "acme-corp")
    private String companyCode;

    @Schema(description = "Official company email address", example = "contact@acme.com")
    private String email;

    @Schema(description = "Current status of the tenant", example = "ACTIVE")
    private String status;

    @Schema(description = "Full name of the initial admin user", example = "John Doe")
    private String adminFullName;

    @Schema(description = "Email address of the initial admin user", example = "admin@acme.com")
    private String adminEmail;

    @Schema(description = "Username for the initial admin user", example = "admin")
    private String adminUsername;

    @Schema(description = "Timestamp when the tenant was created")
    private LocalDateTime createdAt;

    @Schema(description = "User who created the tenant record")
    private String createdBy;

    @Schema(description = "Timestamp when the tenant record was last modified")
    private LocalDateTime updatedAt;

    @Schema(description = "User who last modified the tenant record")
    private String updatedBy;
}

