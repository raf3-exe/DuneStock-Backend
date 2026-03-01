package com.dunestock.api.repository;

import com.dunestock.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    // 🌟 เพิ่มบรรทัดนี้ เพื่อให้หาหมวดหมู่เฉพาะของโกดังนั้นๆ ได้
    List<Category> findByWarehouse_WarehouseId(String warehouseId);
}