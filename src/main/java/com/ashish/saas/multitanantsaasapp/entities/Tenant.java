package com.ashish.saas.multitanantsaasapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "tenants")
public class Tenant extends AbstractEntity {

    @Column(name = "company name", nullable = false)
    private String companyName;

    @Column(name = "company code", nullable = false, unique = true)
    private String companyCode;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status = TenantStatus.PENDING;

    // initial admin credentials
    @Column(name = "admin full name", nullable = false)
    private String adminFullName;

    @Column(name = "admin email", nullable = false, unique = true)
    private String adminEmail;

    @Column(name = "admin username", nullable = false, unique = true)
    private String adminUsername;

    @Column(name = "admin password", nullable = false)
    private String adminPassword;

}
