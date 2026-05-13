package com.ashish.saas.multitanantsaasapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for creating or updating a tenant")
public class TenantRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    @Schema(description = "Company display name", example = "Acme Corporation", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @NotBlank(message = "Company code is required")
    @Size(min = 2, max = 50, message = "Company code must be between 2 and 50 characters")
    @Schema(description = "Unique company identifier", example = "acme-corp", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyCode;

    @NotBlank(message = "Company email is required")
    @Email(message = "Company email must be a valid email address")
    @Schema(description = "Official company email address", example = "contact@acme.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Admin full name is required")
    @Size(min = 2, max = 100, message = "Admin full name must be between 2 and 100 characters")
    @Schema(description = "Full name of the initial admin user", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adminFullName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be a valid email address")
    @Schema(description = "Email address of the initial admin user", example = "admin@acme.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adminEmail;

    @NotBlank(message = "Admin username is required")
    @Size(min = 3, max = 50, message = "Admin username must be between 3 and 50 characters")
    @Schema(description = "Username for the initial admin user", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adminUsername;

    @NotBlank(message = "Admin password is required")
    @Size(min = 8, message = "Admin password must be at least 8 characters long")
    @Schema(description = "Password for the initial admin user (will be encrypted)", example = "SecurePassword123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adminPassword;
}

