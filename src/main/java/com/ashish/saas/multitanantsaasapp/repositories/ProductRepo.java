package com.ashish.saas.multitanantsaasapp.repositories;

import com.ashish.saas.multitanantsaasapp.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepo extends JpaRepository<Product,String> {
    Optional<Product> findByReferenceIgnoreCase(String reference);
}
