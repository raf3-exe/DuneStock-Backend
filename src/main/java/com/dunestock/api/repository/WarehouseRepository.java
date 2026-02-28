package com.dunestock.api.repository;

import com.dunestock.api.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
    List<Warehouse> findByOwnerUserId(String userId);
}