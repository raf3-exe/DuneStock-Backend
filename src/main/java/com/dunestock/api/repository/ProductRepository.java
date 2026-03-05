package com.dunestock.api.repository;

import com.dunestock.api.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByWarehouseWarehouseId(String warehouseId);
    Page<Product> findByWarehouse_WarehouseId(String warehouseId, Pageable pageable);

    // 🌟 เปลี่ยนมาดึงเฉพาะสินค้าที่ isDeleted = false (ยังไม่ถูกลบ)
    Page<Product> findByWarehouse_WarehouseIdAndIsDeletedFalse(String warehouseId, Pageable pageable);
    Page<Product> findByIsDeletedFalse(Pageable pageable);


}
