package com.ashish.saas.multitanantsaasapp.entities;

import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "categories")
// @Filter must sit on the concrete @Entity: Hibernate does not inherit it from the
// @MappedSuperclass (AbstractEntity), so without this the tenantFilter never applied
// and every query returned rows from all tenants.
@Filter(name = "tenantFilter")
public class Category extends AbstractEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
}
