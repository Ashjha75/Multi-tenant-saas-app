package com.ashish.saas.multitanantsaasapp.common;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
public class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id",nullable = false, updatable = false)
    private String id;

    @Column(name="tenant_id",nullable = false, unique = true)
    private String tenantId;
}
