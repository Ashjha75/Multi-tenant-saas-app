package com.ashish.saas.multitanantsaasapp.entities;
import com.ashish.saas.multitanantsaasapp.common.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Auditable;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="categories")
public class Category extends AbstractEntity {
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
}
