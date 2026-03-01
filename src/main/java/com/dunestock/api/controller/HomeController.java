package com.dunestock.api.controller;

import com.dunestock.api.model.StockHistory;
import com.dunestock.api.model.User;
import com.dunestock.api.model.Warehouse;
import com.dunestock.api.repository.StockHistoryRepository;
import com.dunestock.api.repository.UserRepository;
import com.dunestock.api.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HomeController {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private UserRepository userRepository;



    @GetMapping("/warehouses")
    public ResponseEntity<?> getWarehouses(@RequestParam String userId) {
        List<Warehouse> warehouses = warehouseRepository.findByOwnerUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Warehouse w : warehouses) {
            Map<String, Object> map = new HashMap<>();
            map.put("warehouse_id",      w.getWarehouseId());
            map.put("warehouse_name",    w.getWarehouseName());
            map.put("warehouses_detail", w.getWarehouseDetail() != null ? w.getWarehouseDetail() : "");
            map.put("create_at",         w.getCreatedAt() != null ? w.getCreatedAt().toString() : "");
            map.put("owner_id",          w.getOwner().getUserId());
            map.put("owner_username",    w.getOwner().getUsername()); // ✅ เพิ่มต่อจากบรรทัดนี้
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stock-history")
    public ResponseEntity<?> getStockHistory(@RequestParam String userId) {
        List<StockHistory> historyList = stockHistoryRepository.findByUserUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (StockHistory h : historyList) {
            Map<String, Object> map = new HashMap<>();
            map.put("stock_history_id", h.getStockHistoryId());
            map.put("amount", h.getAmount());
            map.put("type", h.getType().name());
            map.put("warehouses_detail", "");
            map.put("create_at", h.getCreatedAt() != null ? h.getCreatedAt().toString() : "");
            map.put("product_id", h.getProduct().getProductId());
            map.put("user_id", h.getUser().getUserId());
            map.put("product_name", h.getProduct().getProductName() != null ? h.getProduct().getProductName() : "");
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // POST /api/warehouses
    @PostMapping("/warehouses")
    public ResponseEntity<?> createWarehouse(@RequestBody Map<String, String> body) {
        try {
            String ownerId = body.get("owner_id");
            String whName = body.get("warehouse_name");
            String whDetail = body.get("warehouses_detail");

            // หา User จาก owner_id
            User owner = userRepository.findById(ownerId).orElse(null);
            if (owner == null) {
                return ResponseEntity.status(404).body("ไม่พบผู้ใช้ id: " + ownerId);
            }

            // สร้าง Warehouse ใหม่
            Warehouse warehouse = new Warehouse();
            warehouse.setWarehouseId("W" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
            warehouse.setWarehouseName(whName);
            warehouse.setWarehouseDetail(whDetail);
            warehouse.setCreatedAt(LocalDateTime.now());
            warehouse.setOwner(owner);

            Warehouse saved = warehouseRepository.save(warehouse);

            // ส่งกลับ format เดียวกับ getWarehouses
            Map<String, Object> result = new HashMap<>();
            result.put("warehouse_id", saved.getWarehouseId());
            result.put("warehouse_name", saved.getWarehouseName());
            result.put("warehouses_detail", saved.getWarehouseDetail() != null ? saved.getWarehouseDetail() : "");
            result.put("create_at", saved.getCreatedAt().toString());
            result.put("owner_id", saved.getOwner().getUserId());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("สร้างไม่สำเร็จ: " + e.getMessage());
        }
    }

    // =========================================================
    // 🌟 แก้ไขฟังก์ชันนี้ เพื่อส่งข้อมูลให้ทั้งคุณและเพื่อนใช้งานได้
    // =========================================================
    // GET /api/warehouses/{warehouseId}/products
    @GetMapping("/warehouses/{warehouseId}/products")
    public ResponseEntity<?> getProductsByWarehouse(@PathVariable String warehouseId) {
        try {
            Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            if (warehouse == null) {
                return ResponseEntity.status(404).body("ไม่พบโกดัง");
            }

            List<Map<String, Object>> result = new ArrayList<>();

            if (warehouse.getProducts() != null) {
                for (var product : warehouse.getProducts()) {
                    Map<String, Object> map = new HashMap<>();

                    // ------------------------------------------
                    // 🧑‍💻 1. ชุดข้อมูลสำหรับแอปของ "เพื่อนคุณ" (มี _)
                    // ------------------------------------------
                    map.put("product_id",    product.getProductId());
                    map.put("product_name",  product.getProductName() != null ? product.getProductName() : "");
                    map.put("category_name", product.getCategory() != null ? product.getCategory().getCategoryName() : null);

                    // ------------------------------------------
                    // 🙋‍♂️ 2. ชุดข้อมูลสำหรับแอปของ "คุณ" (CamelCase)
                    // ------------------------------------------
                    map.put("productId",     product.getProductId());
                    map.put("productName",   product.getProductName() != null ? product.getProductName() : "");
                    map.put("sku",           product.getSku() != null ? product.getSku() : "");
                    map.put("createdAt",     product.getCreatedAt() != null ? product.getCreatedAt().toString() : "");

                    // สร้างโครงสร้าง Category Object เพื่อแอปของคุณ
                    if (product.getCategory() != null) {
                        Map<String, Object> catMap = new HashMap<>();
                        catMap.put("categoryId", product.getCategory().getCategoryId());
                        catMap.put("categoryName", product.getCategory().getCategoryName());
                        map.put("category", catMap);
                    }

                    // ------------------------------------------
                    // 🤝 3. ข้อมูลที่ใช้ร่วมกันได้ (ชื่อเหมือนกัน)
                    // ------------------------------------------
                    map.put("quantity",      product.getQuantity());

                    result.add(map);
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // GET /api/users/{userId}
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return ResponseEntity.status(404).body("ไม่พบผู้ใช้");

            Map<String, Object> result = new HashMap<>();
            result.put("user_id",   user.getUserId());
            result.put("username",  user.getUsername());
            result.put("email",     user.getEmail());
            result.put("create_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
            result.put("update_at", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "");
            result.put("delete_at", user.getDeletedAt() != null ? user.getDeletedAt().toString() : "");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    @Transactional
    @DeleteMapping("/warehouses/{warehouseId}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable String warehouseId) {
        try {
            if (!warehouseRepository.existsById(warehouseId)) {
                return ResponseEntity.status(404).body("ไม่พบโกดัง");
            }
            warehouseRepository.deleteById(warehouseId);
            return ResponseEntity.ok("ลบสำเร็จ");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("ลบไม่สำเร็จ: " + e.getMessage());
        }
    }
}