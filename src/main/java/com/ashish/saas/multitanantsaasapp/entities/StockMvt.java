package com.ashish.saas.multitanantsaasapp.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stock_mvts")
// @Filter must sit on the concrete @Entity: Hibernate does not inherit it from the
// @MappedSuperclass (AbstractEntity), so without this the tenantFilter never applied
// and every query returned rows from all tenants.
@Filter(name = "tenantFilter")
public class StockMvt extends AbstractEntity {
    @Column(name = "type_mvt", nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeMvt typeMvt;


    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "date_mvt", nullable = false)
    private LocalDate dateMvt;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
