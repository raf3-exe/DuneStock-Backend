package com.dunestock.api.controller;

import com.dunestock.api.model.Category;
import com.dunestock.api.model.Product;
import com.dunestock.api.model.Warehouse;
import com.dunestock.api.repository.CategoryRepository;
import com.dunestock.api.repository.ProductRepository;
import com.dunestock.api.repository.WarehouseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // 🌟 Import สำหรับการทำงานต่อเนื่อง
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
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository; // 🌟 1. เพิ่ม ProductRepository เข้ามาเพื่อใช้แก้ไขสินค้า

    // อัปเดต Constructor ให้รับ ProductRepository ด้วย
    public CategoryController(CategoryRepository categoryRepository,
                              WarehouseRepository warehouseRepository,
                              ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<?> getCategoriesByWarehouse(@PathVariable String warehouseId) {
        List<Category> categories = categoryRepository.findByWarehouse_WarehouseId(warehouseId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Category c : categories) {
            Map<String, Object> map = new HashMap<>();
            map.put("categoryId", c.getCategoryId());
            map.put("categoryName", c.getCategoryName());

            long activeCount = 0;
            if (c.getProducts() != null) {
                activeCount = c.getProducts().stream()
                        .filter(p -> !p.isDeleted()) // นับเฉพาะตัวที่ยังใช้งานอยู่
                        .count();
            }
            map.put("productCount", (int) activeCount);

            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/newCategory/{warehouseId}")
    public ResponseEntity<?> createCategory(@PathVariable String warehouseId, @RequestBody Category categoryRequest) {
        try {
            Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            if (warehouse == null) {
                return ResponseEntity.badRequest().body("ไม่พบโกดังรหัส: " + warehouseId);
            }

            List<Category> all = categoryRepository.findAll();
            int maxNum = all.stream()
                    .map(c -> c.getCategoryId().replaceAll("[^0-9]", ""))
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .max().orElse(0);

            categoryRequest.setCategoryId(String.format("C%03d", maxNum + 1));
            categoryRequest.setWarehouse(warehouse);
            categoryRequest.setCreatedAt(LocalDateTime.now());

            Category saved = categoryRepository.save(categoryRequest);

            Map<String, Object> map = new HashMap<>();
            map.put("categoryId", saved.getCategoryId());
            map.put("categoryName", saved.getCategoryName());
            map.put("productCount", 0);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // 🌟 2. อัปเดตฟังก์ชันลบหมวดหมู่
    @DeleteMapping("/delete/{id}")
    @Transactional // 🌟 ใส่เพื่อให้ทำงานรวดเดียวจบ ป้องกันข้อมูลพังกลางคัน
    public ResponseEntity<?> deleteCategory(@PathVariable String id) {
        try {
            Category category = categoryRepository.findById(id).orElse(null);
            if (category == null) return ResponseEntity.notFound().build();

            long activeProductCount = 0;
            if (category.getProducts() != null) {
                activeProductCount = category.getProducts().stream()
                        .filter(p -> !p.isDeleted()) // เช็กเฉพาะสินค้าที่ยังใช้งานอยู่
                        .count();
            }

            // ถ้ายังมีสินค้าที่ "ใช้งานอยู่" จะไม่อนุญาตให้ลบหมวดหมู่
            if (activeProductCount > 0) {
                return ResponseEntity.badRequest().body("ไม่สามารถลบได้ เนื่องจากมีสินค้าใช้งานอยู่ในประเภทนี้ " + activeProductCount + " รายการ");
            }

            // 🌟 3. ปลดล็อกสินค้าที่ "ถูกซ่อน (Soft Delete)" ไปแล้ว
            // ให้กลายเป็น "ไม่ระบุประเภท" (null) เพื่อไม่ให้ Database แจ้ง Error Foreign Key
            if (category.getProducts() != null) {
                for (Product p : category.getProducts()) {
                    p.setCategory(null); // ตัดความสัมพันธ์กับหมวดหมู่นี้
                    productRepository.save(p); // เซฟการเปลี่ยนแปลง
                }
            }

            // 4. พอล้างความสัมพันธ์เสร็จแล้ว ก็สามารถลบหมวดหมู่นี้ทิ้งได้เลย!
            categoryRepository.deleteById(id);

            return ResponseEntity.ok(Map.of("message", "ลบสำเร็จ"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable String id, @RequestBody Category categoryDetails) {
        try {
            Category category = categoryRepository.findById(id).orElse(null);
            if (category == null) {
                return ResponseEntity.notFound().build();
            }

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