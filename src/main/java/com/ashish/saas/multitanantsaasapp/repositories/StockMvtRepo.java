package com.ashish.saas.multitanantsaasapp.repositories;

import com.ashish.saas.multitanantsaasapp.entities.StockMvt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMvtRepo extends JpaRepository<StockMvt, String> {
}
