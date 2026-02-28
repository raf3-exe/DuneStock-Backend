package com.dunestock.api.controller;

import com.dunestock.api.model.StockHistory;
import com.dunestock.api.model.Warehouse;
import com.dunestock.api.repository.StockHistoryRepository;
import com.dunestock.api.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HomeController {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

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
            map.put("amount",           h.getAmount());
            map.put("type",             h.getType().name());
            map.put("warehouses_detail","");
            map.put("create_at",        h.getCreatedAt() != null ? h.getCreatedAt().toString() : "");
            map.put("product_id",       h.getProduct().getProductId());
            map.put("user_id",          h.getUser().getUserId());
            map.put("product_name",     h.getProduct().getProductName() != null ? h.getProduct().getProductName() : "");
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }
}