package com.dunestock.api.repository;

import com.dunestock.api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByWarehouseWarehouseId(String warehouseId);
    

}
