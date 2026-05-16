package com.ashish.saas.multitanantsaasapp.controllers;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.TenantRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.TenantResponse;
import com.ashish.saas.multitanantsaasapp.services.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenants", description = "Tenant onboarding and lifecycle management")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @Operation(summary = "Register a new tenant", description = "Creates a tenant in PENDING state and stores admin details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or tenant already exists")
    })
    public ResponseEntity<Void> registerTenant(@Valid @RequestBody final TenantRequest request) {
        this.tenantService.registerTenant(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tenant-id}/approve")
    @Operation(summary = "Approve a tenant", description = "Approves a tenant and provisions resources.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant approved successfully"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<Void> approveTenant(
            @Parameter(description = "UUID of the tenant to approve")
            @PathVariable("tenant-id") final String tenantId
    ) {
        this.tenantService.approveTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tenant-id}/activate")
    @Operation(summary = "Activate a tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant activated successfully"),
            @ApiResponse(responseCode = "400", description = "Tenant is not in pending status"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<Void> activateTenant(
            @Parameter(description = "UUID of the tenant to activate")
            @PathVariable("tenant-id") final String tenantId
    ) {
        this.tenantService.activateTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tenant-id}/deactivate")
    @Operation(summary = "Deactivate a tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant deactivated successfully"),
            @ApiResponse(responseCode = "400", description = "Tenant is not in active status"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<Void> deactivateTenant(
            @Parameter(description = "UUID of the tenant to deactivate")
            @PathVariable("tenant-id") final String tenantId
    ) {
        this.tenantService.deactivateTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tenant-id}/suspend")
    @Operation(summary = "Suspend a tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant suspended successfully"),
            @ApiResponse(responseCode = "400", description = "Tenant is not in active status"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<Void> suspendTenant(
            @Parameter(description = "UUID of the tenant to suspend")
            @PathVariable("tenant-id") final String tenantId
    ) {
        this.tenantService.suspendTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "List tenants (paginated)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenants found",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    public ResponseEntity<PageResponse<TenantResponse>> getAllTenants(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "10") final int size
    ) {
        return ResponseEntity.ok(this.tenantService.findAll(page, size));
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending tenants (paginated)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending tenants found",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMINISTRATOR')")
    public ResponseEntity<PageResponse<TenantResponse>> getPendingTenants(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "10") final int size
    ) {
        return ResponseEntity.ok(this.tenantService.findPending(page, size));
    }
}

