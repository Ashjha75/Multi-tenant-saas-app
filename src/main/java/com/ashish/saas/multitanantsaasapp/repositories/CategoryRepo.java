package com.ashish.saas.multitanantsaasapp.repositories;

import com.ashish.saas.multitanantsaasapp.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepo extends  JpaRepository<Category, String>{
}
