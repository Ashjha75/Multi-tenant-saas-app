package com.ashish.saas.multitanantsaasapp.security;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(final String message) {
        super(message);
    }
}

