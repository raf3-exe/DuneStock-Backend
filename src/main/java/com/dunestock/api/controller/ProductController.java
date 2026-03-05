package com.dunestock.api.controller;

import com.dunestock.api.model.*;
import com.dunestock.api.repository.*;
import jakarta.transaction.Transactional;
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
    private final UserRepository userRepository;

    public ProductController(ProductRepository productRepository,
                             WarehouseRepository warehouseRepository,
                             CategoryRepository categoryRepository,
                             StockHistoryRepository stockHistoryRepository,
                             UserRepository userRepository) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.categoryRepository = categoryRepository;
        this.stockHistoryRepository = stockHistoryRepository;
        this.userRepository = userRepository;
    }

    private void recordStockHistory(Product product, Warehouse warehouse, int amount, StockHistory.StockType type, String userId) {
        if (amount <= 0) return;

        StockHistory history = new StockHistory();
        long count = stockHistoryRepository.count();
        history.setStockHistoryId(String.format("SH%03d", count + 1));
        history.setProduct(product);
        history.setWarehouse(warehouse);
        history.setAmount(amount);
        history.setType(type);

        history.setCreatedAt(LocalDateTime.now());
        if (userId != null && !userId.isEmpty()) {
            User user = userRepository.findById(userId).orElse(null);
            history.setUser(user);
        }

        stockHistoryRepository.save(history);
        System.out.println("✅ บันทึกประวัติสำเร็จ: " + type + " จำนวน " + amount + " โดย User: " + userId);
    }

    private Map<String, Object> convertToMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", product.getProductId());
        map.put("productName", product.getProductName());
        map.put("sku", product.getSku());
        map.put("quantity", product.getQuantity());
        map.put("createdAt", product.getCreatedAt() != null ? product.getCreatedAt().toString() : "");
        map.put("isDeleted", product.isDeleted());
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

        // 🌟 ดึงเฉพาะสินค้าที่ยังไม่โดนลบ
        if (warehouseId != null && !warehouseId.isEmpty()) {
            productPage = productRepository.findByWarehouse_WarehouseIdAndIsDeletedFalse(warehouseId, pageable);
        } else {
            productPage = productRepository.findByIsDeletedFalse(pageable);
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

            recordStockHistory(saved, saved.getWarehouse(), saved.getQuantity(), StockHistory.StockType.IN, userId);

            return ResponseEntity.ok(convertToMap(saved));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/updateProduct/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody Product productDetails, @RequestParam(required = false) String userId) {
        try {
            Product existingProduct = productRepository.findById(id).orElse(null);
            // 🌟 ตรวจสอบด้วยว่าสินค้าโดนลบไปหรือยัง
            if (existingProduct == null || existingProduct.isDeleted()) {
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

    // 🌟 เปลี่ยนระบบลบ เป็นระบบ "ซ่อน (Soft Delete)"
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id, @RequestParam(required = false) String userId) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null && !product.isDeleted()) {
            try {
                // เก็บจำนวนสินค้าที่มีอยู่เพื่อบันทึกประวัติว่าเอาออกไปเท่าไหร่
                int qtyToRemove = product.getQuantity();

                // ตั้งค่าเป็นถูกลบ และให้จำนวนเป็น 0
                product.setDeleted(true);
                product.setQuantity(0);
                productRepository.save(product);

                // บันทึกประวัติว่าผู้ใช้คนนี้ลบของชิ้นนี้
                if (qtyToRemove > 0) {
                    recordStockHistory(product, product.getWarehouse(), qtyToRemove, StockHistory.StockType.OUT, userId);
                }

                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Error deleting product: " + e.getMessage());
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}