package com.dunestock.api.controller;

import com.dunestock.api.model.Category;
import com.dunestock.api.model.Warehouse;
import com.dunestock.api.repository.CategoryRepository;
import com.dunestock.api.repository.WarehouseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository; // ต้องใช้เพื่อผูกหมวดหมู่เข้าโกดัง

    public CategoryController(CategoryRepository categoryRepository, WarehouseRepository warehouseRepository) {
        this.categoryRepository = categoryRepository;
        this.warehouseRepository = warehouseRepository;
    }

    // 🌟 1. ดึงประเภทสินค้าของโกดังนั้นๆ + นับจำนวนสินค้า
    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<?> getCategoriesByWarehouse(@PathVariable String warehouseId) {
        List<Category> categories = categoryRepository.findByWarehouse_WarehouseId(warehouseId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Category c : categories) {
            Map<String, Object> map = new HashMap<>();
            map.put("categoryId", c.getCategoryId());
            map.put("categoryName", c.getCategoryName());

            // 🌟 พระเอกของเรา: นับจำนวนสินค้าในหมวดหมู่นี้
            int count = (c.getProducts() != null) ? c.getProducts().size() : 0;
            map.put("productCount", count); // ส่งจำนวนรายการกลับไปโชว์

            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // 🌟 2. เพิ่มประเภทสินค้าใหม่ เข้าโกดังที่กำหนด
    @PostMapping("/newCategory/{warehouseId}")
    public ResponseEntity<?> createCategory(@PathVariable String warehouseId, @RequestBody Category categoryRequest) {
        try {
            // เช็กโกดังก่อนว่ามีจริงไหม
            Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            if (warehouse == null) {
                return ResponseEntity.badRequest().body("ไม่พบโกดังรหัส: " + warehouseId);
            }

            // รันเลข C001
            List<Category> all = categoryRepository.findAll();
            int maxNum = all.stream()
                    .map(c -> c.getCategoryId().replaceAll("[^0-9]", ""))
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .max().orElse(0);

            categoryRequest.setCategoryId(String.format("C%03d", maxNum + 1));
            categoryRequest.setWarehouse(warehouse); // ผูกโกดัง
            categoryRequest.setCreatedAt(LocalDateTime.now());

            Category saved = categoryRepository.save(categoryRequest);

            Map<String, Object> map = new HashMap<>();
            map.put("categoryId", saved.getCategoryId());
            map.put("categoryName", saved.getCategoryName());
            map.put("productCount", 0); // เพิ่งสร้างใหม่ จำนวนสินค้าต้องเป็น 0

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // 🌟 3. ลบประเภทสินค้า
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable String id) {
        try {
            Category category = categoryRepository.findById(id).orElse(null);
            if (category == null) return ResponseEntity.notFound().build();

            // 🚨 ดักไว้ไม่ให้ลบ ถ้ายังมีสินค้าอยู่ในหมวดหมู่นี้ (ไม่งั้นข้อมูลสินค้าจะพัง)
            if (category.getProducts() != null && !category.getProducts().isEmpty()) {
                return ResponseEntity.badRequest().body("ไม่สามารถลบได้ เนื่องจากมีสินค้าอยู่ในประเภทนี้ " + category.getProducts().size() + " รายการ");
            }

            categoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "ลบสำเร็จ"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    // 🌟 4. แก้ไขชื่อประเภทสินค้า
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable String id, @RequestBody Category categoryDetails) {
        try {
            Category category = categoryRepository.findById(id).orElse(null);
            if (category == null) {
                return ResponseEntity.notFound().build();
            }

            // อัปเดตชื่อประเภทใหม่
            category.setCategoryName(categoryDetails.getCategoryName());
            categoryRepository.save(category);

            Map<String, String> response = new HashMap<>();
            response.put("message", "อัปเดตสำเร็จ");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}