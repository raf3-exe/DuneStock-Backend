package com.dunestock.api.controller;

import com.dunestock.api.model.*;
import com.dunestock.api.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
public class ProductController {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final CategoryRepository categoryRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final UserRepository userRepository; // 🌟 1. เพิ่มตัวช่วยค้นหา User

    public ProductController(ProductRepository productRepository,
                             WarehouseRepository warehouseRepository,
                             CategoryRepository categoryRepository,
                             StockHistoryRepository stockHistoryRepository,
                             UserRepository userRepository) { // 🌟 รับ UserRepository
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.categoryRepository = categoryRepository;
        this.stockHistoryRepository = stockHistoryRepository;
        this.userRepository = userRepository;
    }

    // 🌟 2. อัปเดตฟังก์ชันบันทึกประวัติให้รับ userId
    private void recordStockHistory(Product product, Warehouse warehouse, int amount, StockHistory.StockType type, String userId) {
        if (amount <= 0) return;

        StockHistory history = new StockHistory();
        long count = stockHistoryRepository.count();
        history.setStockHistoryId(String.format("SH%03d", count + 1));
        history.setProduct(product);
        history.setWarehouse(warehouse);
        history.setAmount(amount);
        history.setType(type);

        // 🌟 3. ค้นหา User จาก ID ที่ส่งมาและผูกกับประวัติ
        if (userId != null && !userId.isEmpty()) {
            User user = userRepository.findById(userId).orElse(null);
            history.setUser(user);
        }

        stockHistoryRepository.save(history);
        System.out.println("✅ บันทึกประวัติสำเร็จ: " + type + " จำนวน " + amount + " โดย User: " + userId);
    }

    // Method แปลงข้อมูล (คงเดิม)
    private Map<String, Object> convertToMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", product.getProductId());
        map.put("productName", product.getProductName());
        map.put("sku", product.getSku());
        map.put("quantity", product.getQuantity());
        map.put("createdAt", product.getCreatedAt() != null ? product.getCreatedAt().toString() : "");

        if (product.getCategory() != null) {
            Map<String, Object> catMap = new HashMap<>();
            catMap.put("categoryId", product.getCategory().getCategoryId());
            catMap.put("categoryName", product.getCategory().getCategoryName());
            map.put("category", catMap);
        }

        if (product.getWarehouse() != null) {
            Map<String, Object> whMap = new HashMap<>();
            whMap.put("warehouseId", product.getWarehouse().getWarehouseId());
            whMap.put("warehouseName", product.getWarehouse().getWarehouseName());
            map.put("warehouse", whMap);
        }
        return map;
    }

    @GetMapping("/showAll")
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("productId").ascending());
        Page<Product> productPage;

        if (warehouseId != null && !warehouseId.isEmpty()) {
            productPage = productRepository.findByWarehouse_WarehouseId(warehouseId, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        List<Map<String, Object>> products = productPage.getContent().stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        response.put("currentPage", productPage.getNumber() + 1);
        response.put("totalPages", productPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // 🌟 4. รับค่า userId ตอนเพิ่มสินค้า
    @PostMapping("/newProduct")
    public ResponseEntity<?> createProduct(@RequestBody Product productRequest, @RequestParam(required = false) String userId) {
        try {
            List<Product> all = productRepository.findAll();
            int maxNum = all.stream()
                    .map(p -> p.getProductId().replaceAll("[^0-9]", ""))
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .max().orElse(0);
            productRequest.setProductId(String.format("P%04d", maxNum + 1));

            if (productRequest.getWarehouse() != null) {
                String whId = productRequest.getWarehouse().getWarehouseId();
                if (whId != null && !whId.isEmpty()) {
                    Warehouse realWh = warehouseRepository.findById(whId).orElse(null);
                    productRequest.setWarehouse(realWh);
                }
            }

            if (productRequest.getCategory() != null && productRequest.getCategory().getCategoryId() != null) {
                String catId = productRequest.getCategory().getCategoryId();
                Category realCat = categoryRepository.findById(catId).orElse(null);
                productRequest.setCategory(realCat);
            }

            productRequest.setCreatedAt(LocalDateTime.now());
            Product saved = productRepository.save(productRequest);

            // 🌟 ส่ง userId เข้าไปบันทึกด้วย
            recordStockHistory(saved, saved.getWarehouse(), saved.getQuantity(), StockHistory.StockType.IN, userId);

            return ResponseEntity.ok(convertToMap(saved));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // 🌟 5. รับค่า userId ตอนอัปเดตสินค้า
    @PutMapping("/updateProduct/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody Product productDetails, @RequestParam(required = false) String userId) {
        try {
            Product existingProduct = productRepository.findById(id).orElse(null);
            if (existingProduct == null) {
                return ResponseEntity.notFound().build();
            }

            int oldQty = existingProduct.getQuantity();
            int newQty = productDetails.getQuantity();
            int diff = newQty - oldQty;

            existingProduct.setProductName(productDetails.getProductName());
            existingProduct.setQuantity(newQty);

            if (productDetails.getCategory() != null && productDetails.getCategory().getCategoryId() != null) {
                Category realCategory = categoryRepository.findById(productDetails.getCategory().getCategoryId()).orElse(null);
                existingProduct.setCategory(realCategory);
            }

            Product saved = productRepository.save(existingProduct);

            // 🌟 ส่ง userId เข้าไปบันทึกด้วย
            if (diff > 0) {
                recordStockHistory(saved, saved.getWarehouse(), diff, StockHistory.StockType.IN, userId);
            } else if (diff < 0) {
                recordStockHistory(saved, saved.getWarehouse(), Math.abs(diff), StockHistory.StockType.OUT, userId);
            }

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "อัปเดตข้อมูลสำเร็จ");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}