package com.dunestock.api.controller;

import com.dunestock.api.model.Product;
import com.dunestock.api.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 🌟 Method แปลงข้อมูลส่งให้ Android
    private Map<String, Object> convertToMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", product.getProductId());
        map.put("productName", product.getProductName());
        map.put("sku", product.getSku());
        map.put("quantity", product.getQuantity());
        map.put("createdAt", product.getCreatedAt());

        // 🌟 1. จัด Category เป็น Object เพื่อให้ Android โชว์ประเภทสินค้าได้
        if (product.getCategory() != null) {
            Map<String, Object> catMap = new HashMap<>();
            catMap.put("categoryId", product.getCategory().getCategoryId());
            catMap.put("categoryName", product.getCategory().getCategoryName());
            map.put("category", catMap);
        }

        // 🌟 2. จำลองข้อมูล Warehouse ส่งกลับไปให้ Android ชั่วคราว (กันแอปฝั่งนู้นพัง)
        Map<String, Object> whMap = new HashMap<>();
        whMap.put("warehouseId", "W001");
        whMap.put("warehousesDetail", "โกดังหลัก (จำลอง)");
        map.put("warehouse", whMap);

        return map;
    }

    @GetMapping("/showAll")
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        List<Product> products = productRepository.findAll();
        List<Map<String, Object>> response = products.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/newProduct")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Product product) {
        // 🌟 3. สำคัญมาก! เคลียร์ค่า Warehouse ทิ้งก่อนเซฟลง Database
        // เพื่อไม่ให้ Hibernate วิ่งไปหาโกดัง W001 ที่ยังไม่มีอยู่จริง แล้วเกิด Error 500
        product.setWarehouse(null);

        Product savedProduct = productRepository.save(product);

        Map<String, Object> response = convertToMap(savedProduct);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/updateProduct/{id}")
    public ResponseEntity<Map<String, String>> updateProduct(@PathVariable String id, @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setProductName(productDetails.getProductName());
                    product.setQuantity(productDetails.getQuantity());

                    // อัปเดต Category
                    if (productDetails.getCategory() != null) {
                        product.setCategory(productDetails.getCategory());
                    }

                    // 🌟 ไม่ต้องอัปเดต Warehouse ปล่อยผ่านไปก่อน

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