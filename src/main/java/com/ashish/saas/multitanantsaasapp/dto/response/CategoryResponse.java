package com.ashish.saas.multitanantsaasapp.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private String name;
    private String description;

}
