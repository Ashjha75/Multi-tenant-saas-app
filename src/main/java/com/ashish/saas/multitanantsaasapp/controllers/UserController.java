package com.ashish.saas.multitanantsaasapp.controllers;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.UserRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.UserResponse;
import com.ashish.saas.multitanantsaasapp.entities.UserRole;
import com.ashish.saas.multitanantsaasapp.services.UserService;
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

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management within a tenant")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Void> createUser(@Valid @RequestBody final UserRequest request) {
        this.userService.createUser(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{user-id}")
    @Operation(summary = "Update a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','ADMINISTRATOR')")
    public ResponseEntity<Void> updateUser(
            @Valid @RequestBody final UserRequest request,
            @Parameter(description = "UUID of the user to update")
            @PathVariable("user-id") final String userId
    ) {
        this.userService.updateUser(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{user-id}")
    @Operation(summary = "Get a user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','ADMINISTRATOR')")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "UUID of the user to retrieve")
            @PathVariable("user-id") final String userId
    ) {
        return ResponseEntity.ok(this.userService.getUserById(userId));
    }

    @GetMapping
    @Operation(summary = "List users (paginated)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users found",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "10") final int size
    ) {
        return ResponseEntity.ok(this.userService.getAllUsers(page, size));
    }

    @DeleteMapping("/{user-id}")
    @Operation(summary = "Delete a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "UUID of the user to delete")
            @PathVariable("user-id") final String userId
    ) {
        this.userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{user-id}/enable")
    @Operation(summary = "Enable a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User enabled successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Void> enableUser(
            @Parameter(description = "UUID of the user to enable")
            @PathVariable("user-id") final String userId
    ) {
        this.userService.enableUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{user-id}/disable")
    @Operation(summary = "Disable a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User disabled successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Void> disableUser(
            @Parameter(description = "UUID of the user to disable")
            @PathVariable("user-id") final String userId
    ) {
        this.userService.disableUser(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/roles")
    @Operation(summary = "List user roles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles returned successfully")
    })
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','COMPANY_ADMIN','ADMINISTRATOR')")
    public ResponseEntity<Map<String, String>> getAllUserRoles() {
        final Map<String, String> roles = new LinkedHashMap<>();
        for (final UserRole role : UserRole.values()) {
            roles.put(role.name(), formatRoleLabel(role.name()));
        }
        return ResponseEntity.ok(roles);
    }

    private String formatRoleLabel(final String enumValue) {
        final String[] parts = enumValue.replace('_', ' ').toLowerCase().split(" ");
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            if (!part.isEmpty()) {
                builder.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1));
            }
            if (i < parts.length - 1) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }
}
