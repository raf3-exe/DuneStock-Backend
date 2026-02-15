package com.dunestock.api.repository;

import com.dunestock.api.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehousesRepository extends JpaRepository<Warehouse, String> {
}
