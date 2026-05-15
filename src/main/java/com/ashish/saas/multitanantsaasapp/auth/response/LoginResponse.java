package com.ashish.saas.multitanantsaasapp.auth.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String tokenType;
}
