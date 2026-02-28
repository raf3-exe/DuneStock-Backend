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

    // สร้าง Method ช่วยแปลง Product เป็น Map (JSON แบบสร้างเอง) เพื่อตัดปัญหาการวนลูป
    private Map<String, Object> convertToMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", product.getProductId());
        map.put("productName", product.getProductName());
        map.put("sku", product.getSku());
        map.put("quantity", product.getQuantity());
        map.put("createdAt", product.getCreatedAt());

        // ดึงแค่ข้อมูล String ออกมาส่งกลับไป ไม่ส่งไปเป็น Object
        if (product.getCategory() != null) {
            map.put("categoryId", product.getCategory().getCategoryId());
            map.put("categoryName", product.getCategory().getCategoryName());
        }
        if (product.getWarehouse() != null) {
            map.put("warehouseId", product.getWarehouse().getWarehouseId());
        }
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
        // 1. เซฟลง Database ตามปกติ (เซฟด้วย Object Model แบบเดิม)
        Product savedProduct = productRepository.save(product);

        // 2. จับมาใส่ Map เพื่อเลือกส่งแค่ข้อความแบนๆ กลับไปให้ Android
        Map<String, Object> response = convertToMap(savedProduct);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/updateProduct/{id}")
    public ResponseEntity<Map<String, String>> updateProduct(@PathVariable String id, @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    // ถ้าเจอ ให้เอาข้อมูลใหม่มาทับข้อมูลเดิม
                    product.setProductName(productDetails.getProductName());
                    product.setQuantity(productDetails.getQuantity());
                    product.setCategory(productDetails.getCategory());

                    // บันทึกการเปลี่ยนแปลงลง Database
                    productRepository.save(product);

                    // 🌟 แก้ไขตรงนี้: ไม่ส่ง Object Product กลับไปแล้ว ส่งแค่ข้อความบอกว่าผ่านก็พอ!
                    Map<String, String> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "อัปเดตข้อมูลสำเร็จ");

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // 🌟 เพิ่มคำสั่งนี้สำหรับการลบสินค้า
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        // เช็คก่อนว่ามีสินค้านี้อยู่ใน Database ไหม
        if (productRepository.existsById(id)) {
            // ถ้ามี สั่งลบเลย
            productRepository.deleteById(id);
            return ResponseEntity.ok().build(); // ส่งสถานะ 200 OK กลับไปว่าลบสำเร็จ
        } else {
            return ResponseEntity.notFound().build(); // ถ้าไม่เจอส่ง 404
        }
    }
}