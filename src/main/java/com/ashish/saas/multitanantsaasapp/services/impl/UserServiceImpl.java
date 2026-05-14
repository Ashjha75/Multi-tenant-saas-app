package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.config.TenantContext;
import com.ashish.saas.multitanantsaasapp.dto.request.UserRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.UserResponse;
import com.ashish.saas.multitanantsaasapp.entities.Tenant;
import com.ashish.saas.multitanantsaasapp.entities.User;
import com.ashish.saas.multitanantsaasapp.entities.UserRole;
import com.ashish.saas.multitanantsaasapp.exception.AppException;
import com.ashish.saas.multitanantsaasapp.mapper.UserMapper;
import com.ashish.saas.multitanantsaasapp.repositories.UserRepo;
import com.ashish.saas.multitanantsaasapp.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void createUser(UserRequest request) {
        final String tenantId = TenantContext.getCurrentTenant();

// validate if username already exists
        if (this.userRepo.existsByUsername(request.getUsername())) {
            throw new DuplicateKeyException("Username already exists");
        }

// validate if email already exists
        if (this.userRepo.existsByEmail(request.getEmail())) {
            throw new DuplicateKeyException("Email already exists");
        }
// validate role (cannot be PLATFORM_ADMIN)
        if (request.getRole() == UserRole.ROLE_PLATFORM_ADMIN) {
            throw new AppException(HttpStatus.BAD_REQUEST, "PLATFORM_ADMIN_NOT_ALLOWED", "Platform admin cannot be created");
        }
        final User user = this.userMapper.toEntity(request);
        user.setTenant(Tenant.builder().id(tenantId).build());
        user.setPassword(this.passwordEncoder.encode(request.getPassword()));

        this.userRepo.save(user);

        log.info("User created successfully");
    }

    @Override
    public void updateUser(String id, UserRequest request) {
        final String tenantId = TenantContext.getCurrentTenant();
        log.info("Updating user for tenant: {}", tenantId);

        final User user = this.userRepo.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("User does not exist"));

        // check if user belongs to the tenant
        if (user.getTenantId() == null || !user.getTenantId().equals(tenantId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User does not belong to the tenant");
        }
// check if username is being changed and if it is already taken
        if (!user.getUsername().equals(request.getUsername()) && this.userRepo.existsByUsername(request.getUsername())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

// check if email is being changed and if it is already taken
        if (!user.getEmail().equals(request.getEmail()) && this.userRepo.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        // validate role (cannot be PLATFORM_ADMIN)
        if (request.getRole() == UserRole.ROLE_PLATFORM_ADMIN) {
            throw new AppException(HttpStatus.BAD_REQUEST, "PLATFORM_ADMIN_NOT_ALLOWED", "Platform admin cannot be created");
        }
        // update user details
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        this.userRepo.save(user);
        log.info("User updated successfully");
    }

    @Override
    public void deleteUser(String id) {
        final String tenantId = TenantContext.getCurrentTenant();
        log.info("Deleting user for tenant: {}", tenantId);

        final User user = this.userRepo.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("User does not exist"));

        // check if user belongs to the tenant
        if (user.getTenantId() == null || !user.getTenantId().equals(tenantId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User does not belong to the tenant");
        }
        user.setDeleted(true);
        this.userRepo.save(user);
        log.info("User deleted successfully");
    }

    @Override
    public UserResponse getUserById(String userId) {
        final String tenantId = TenantContext.getCurrentTenant();
        final User user = this.userRepo.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new EntityNotFoundException("User does not exist"));
        // check if user belongs to the tenant
        if (user.getTenantId() == null || !user.getTenantId().equals(tenantId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User does not belong to the tenant");
        }
        return this.userMapper.toResponse(user);
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        final String tenantId = TenantContext.getCurrentTenant();
        final PageRequest pageRequest = PageRequest.of(page, size);
        final Page<User> userPage = this.userRepo.findAllByTenantIdAndNotDeleted(tenantId, pageRequest);
        final Page<UserResponse> userResponses = userPage.map(this.userMapper::toResponse);
        return PageResponse.of(userResponses);
    }

    @Override
    public void enableUser(String userId) {
        final String tenantId = TenantContext.getCurrentTenant();
        final User user = this.userRepo.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new EntityNotFoundException("User does not exist"));

        // check if user belongs to the tenant
        if (user.getTenantId() == null || !user.getTenantId().equals(tenantId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User does not belong to the tenant");
        }
        user.setEnabled(true);
        this.userRepo.save(user);
        log.info("User enabled successfully");
    }

    @Override
    public void disableUser(String userId) {
        final String tenantId = TenantContext.getCurrentTenant();
        final User user = this.userRepo.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new EntityNotFoundException("User does not exist"));

        // check if user belongs to the tenant
        if (user.getTenantId() == null || !user.getTenantId().equals(tenantId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User does not belong to the tenant");
        }
        user.setEnabled(false);
        this.userRepo.save(user);
        log.info("User disabled successfully");
    }


    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return this.userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user was found with: " + username));
    }

}
