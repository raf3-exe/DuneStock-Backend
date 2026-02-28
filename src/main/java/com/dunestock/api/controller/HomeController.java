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
            map.put("warehouse_id", w.getWarehouseId());
            map.put("warehouse_name", w.getWarehouseName());
            map.put("warehouses_detail", w.getWarehouseDetail() != null ? w.getWarehouseDetail() : "");
            map.put("create_at", w.getCreatedAt() != null ? w.getCreatedAt().toString() : "");
            map.put("owner_id", w.getOwner().getUserId());
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