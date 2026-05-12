package com.ashish.saas.multitanantsaasapp.entities;

import com.ashish.saas.multitanantsaasapp.config.TenantContext;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = String.class),
        defaultCondition = "tenant_id = :tenantId AND deleted = false"
)
@Filter(name = "tenantFilter")
public class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    @PrePersist
    public void onCreate() {
        if (this.deleted == null) {
            this.deleted = false;
        }
        // Auto-inject tenant from context; fail loudly to prevent silent cross-tenant writes
        if (this.tenantId == null) {
            final String tenantFromCtx = TenantContext.getCurrentTenant();
            if (tenantFromCtx == null || tenantFromCtx.isBlank()) {
                throw new IllegalStateException(
                        "[TENANT VIOLATION] tenant_id is null and no tenant found in TenantContext. " +
                        "All persists require a valid X-Tenant-ID header.");
            }
            this.tenantId = tenantFromCtx;
        }
    }
}
