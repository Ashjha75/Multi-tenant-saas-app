package com.ashish.saas.multitanantsaasapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "users")
public class User extends AbstractEntity implements UserDetails {
    @ManyToOne(fetch = FetchType. LAZY)
    @JoinColumn(name = "tenant id", foreignKey = @ForeignKey (name = "fk_user_tenant_id"))
    private Tenant tenant;

    @Column (name = "username", nullable = false, unique = true)
    private String username;

    @Column (name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private
    UserRole role;

    @Column(name = "enabled ")
    private boolean enabled;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role.name()));
    }

    public String getTenantId() {
        if (this.tenant != null) {
            return this.tenant.getId();
        }
        return null;

    }
}
