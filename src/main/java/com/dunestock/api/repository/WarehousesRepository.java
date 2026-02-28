package com.dunestock.api.repository;

import com.dunestock.api.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WarehousesRepository extends JpaRepository<Warehouse, String> {
    List<Warehouse> findByOwner_UserId(String userId);
}
