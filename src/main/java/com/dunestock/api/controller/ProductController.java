package com.dunestock.api.controller;

import com.dunestock.api.model.Category;
import com.dunestock.api.model.Product;
import com.dunestock.api.model.Warehouse;
import com.dunestock.api.repository.CategoryRepository;
import com.dunestock.api.repository.ProductRepository;
import com.dunestock.api.repository.WarehouseRepository;
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
    private final WarehouseRepository warehouseRepository; // 🌟 เพิ่มตัวช่วยค้นหาโกดัง
    private final CategoryRepository categoryRepository;   // 🌟 เพิ่มตัวช่วยค้นหาหมวดหมู่

    // 🌟 อัปเดต Constructor ให้รับ Repository เข้ามาให้ครบ
    public ProductController(ProductRepository productRepository,
                             WarehouseRepository warehouseRepository,
                             CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.categoryRepository = categoryRepository;
    }

    // Method แปลงข้อมูลส่งให้ Android (คงไว้เหมือนเดิม)
    private Map<String, Object> convertToMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", product.getProductId());
        map.put("productName", product.getProductName());
        map.put("sku", product.getSku());
        map.put("quantity", product.getQuantity());
        // แปลง LocalDateTime เป็น String เพื่อให้ Android ไม่งง
        map.put("createdAt", product.getCreatedAt() != null ? product.getCreatedAt().toString() : "");

        if (product.getCategory() != null) {
            Map<String, Object> catMap = new HashMap<>();
            catMap.put("categoryId", product.getCategory().getCategoryId());
            catMap.put("categoryName", product.getCategory().getCategoryName());
            map.put("category", catMap);
        }

        if (product.getWarehouse() != null) {
            Map<String, Object> whMap = new HashMap<>();
            // 🚨 สำคัญ: ชื่อ key "warehouseId" ต้องตรงกับ @SerializedName ใน Android
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

    @PostMapping("/newProduct")
    public ResponseEntity<?> createProduct(@RequestBody Product productRequest) {
        try {
            // 1. สร้างรหัส PXXXX (ส่วนนี้ถูกแล้ว)
            List<Product> all = productRepository.findAll();
            int maxNum = all.stream()
                    .map(p -> p.getProductId().replaceAll("[^0-9]", ""))
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .max().orElse(0);
            productRequest.setProductId(String.format("P%04d", maxNum + 1));

            // 🌟 2. ส่วนดึงโกดังที่สำคัญที่สุด
            System.out.println("====== เริ่มขั้นตอนตรวจสอบโกดัง ======");
            if (productRequest.getWarehouse() == null) {
                System.out.println("🚨 ข้อผิดพลาด: Android ไม่ได้ส่งข้อมูลโกดังมาเลย (Warehouse Object is null)");
            } else {
                String whId = productRequest.getWarehouse().getWarehouseId();
                System.out.println("🔍 รหัสโกดังที่ Android ส่งมาคือ: [" + whId + "]");

                if (whId != null && !whId.isEmpty()) {
                    Warehouse realWh = warehouseRepository.findById(whId).orElse(null);
                    if (realWh != null) {
                        productRequest.setWarehouse(realWh);
                        System.out.println("✅ ผูกโกดังกับสินค้าสำเร็จ!");
                    } else {
                        System.out.println("🚨 ข้อผิดพลาด: หาโกดังรหัส [" + whId + "] ไม่เจอใน Database!");
                        // ถ้าหาไม่เจอ ให้เคลียร์ทิ้งไปเลย จะได้ไม่เป็น Null แบบงงๆ
                        productRequest.setWarehouse(null);
                    }
                } else {
                    System.out.println("🚨 ข้อผิดพลาด: Android ส่งโกดังมา แต่รหัสโกดังเป็น Null หรือค่าว่าง!");
                }
            }
            System.out.println("======================================");

            // 3. จัดการหมวดหมู่
            if (productRequest.getCategory() != null && productRequest.getCategory().getCategoryId() != null) {
                String catId = productRequest.getCategory().getCategoryId();
                Category realCat = categoryRepository.findById(catId).orElse(null);
                productRequest.setCategory(realCat);
            }

            productRequest.setCreatedAt(LocalDateTime.now());
            Product saved = productRepository.save(productRequest);

            return ResponseEntity.ok(convertToMap(saved));

        } catch (Exception e) {
            System.out.println("🚨 เกิดข้อผิดพลาดร้ายแรง: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    @PutMapping("/updateProduct/{id}")
    public ResponseEntity<Map<String, String>> updateProduct(@PathVariable String id, @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setProductName(productDetails.getProductName());
                    product.setQuantity(productDetails.getQuantity());

                    if (productDetails.getCategory() != null && productDetails.getCategory().getCategoryId() != null) {
                        Category realCategory = categoryRepository.findById(productDetails.getCategory().getCategoryId()).orElse(null);
                        product.setCategory(realCategory);
                    }

                    productRepository.save(product);

                    Map<String, String> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "อัปเดตข้อมูลสำเร็จ");

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
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