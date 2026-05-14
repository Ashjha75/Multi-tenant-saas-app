package com.ashish.saas.multitanantsaasapp.services;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.UserRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.UserResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserService extends UserDetails {
    void createUser(final UserRequest request);

    void updateUser(final String id, final UserRequest request);

    void deleteUser(final String id);

    UserResponse getUserById(final String userId);

    PageResponse<UserResponse> getAllUsers(final int page, final int size);

    void enableUser(final String userId);

    void disableUser(final String userId);

    UserDetails LoadUserByUsername(String username) throws UsernameNotFoundException;
}
