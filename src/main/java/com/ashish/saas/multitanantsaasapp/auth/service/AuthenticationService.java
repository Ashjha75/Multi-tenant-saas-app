package com.ashish.saas.multitanantsaasapp.auth.service;

import com.ashish.saas.multitanantsaasapp.auth.request.LoginRequest;
import com.ashish.saas.multitanantsaasapp.auth.response.LoginResponse;

public interface AuthenticationService {
    LoginResponse login(LoginRequest request);
}
