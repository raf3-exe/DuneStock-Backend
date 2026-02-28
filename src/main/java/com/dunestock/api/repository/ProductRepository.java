package com.dunestock.api.repository;

import com.dunestock.api.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByWarehouseWarehouseId(String warehouseId);
    Page<Product> findByWarehouse_WarehouseId(String warehouseId, Pageable pageable);


}
