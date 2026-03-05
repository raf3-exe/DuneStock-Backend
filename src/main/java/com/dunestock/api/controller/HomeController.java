package com.dunestock.api.controller;

import com.dunestock.api.model.Membership;
import com.dunestock.api.model.StockHistory;
import com.dunestock.api.model.User;
import com.dunestock.api.model.Warehouse;
import com.dunestock.api.repository.MembershipRepository;
import com.dunestock.api.repository.StockHistoryRepository;
import com.dunestock.api.repository.UserRepository;
import com.dunestock.api.repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StockHistoryRepository stockHistoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MembershipRepository membershipRepository;


    // GET /api/warehouses
    // ✅ อัปเดต: ดึงโกดังทั้งหมดที่คุณเป็นสมาชิกอยู่ (ยกเว้นสถานะ W รอตอบรับ)
    @GetMapping("/warehouses")
    public ResponseEntity<?> getWarehouses(@RequestParam String userId) {
        try {
            List<Membership> memberships = membershipRepository.findByUserUserId(userId);
            List<Map<String, Object>> result = new ArrayList<>();

            for (Membership m : memberships) {
                // ข้ามคำเชิญที่ยังไม่ตอบรับ
                if (m.getRole() == Membership.Role.W) {
                    continue;
                }

                Warehouse w = m.getWarehouse();
                Map<String, Object> map = new HashMap<>();
                map.put("warehouse_id",      w.getWarehouseId());
                map.put("warehouse_name",    w.getWarehouseName());
                map.put("warehouses_detail", w.getWarehouseDetail() != null ? w.getWarehouseDetail() : "");
                map.put("create_at",         w.getCreatedAt() != null ? w.getCreatedAt().toString() : "");
                map.put("owner_id",          w.getOwner() != null ? w.getOwner().getUserId() : "");
                map.put("owner_username",    w.getOwner() != null ? w.getOwner().getUsername() : "");
                map.put("my_role",           m.getRole().name()); // ส่ง role กลับไปด้วยเผื่อใช้ในแอป

                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // GET /api/stock-history
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

    // POST /api/warehouses
    @PostMapping("/warehouses")
    public ResponseEntity<?> createWarehouse(@RequestBody Map<String, String> body) {
        try {
            String ownerId  = body.get("owner_id");
            String whName   = body.get("warehouse_name");
            String whDetail = body.get("warehouses_detail");

            User owner = userRepository.findById(ownerId).orElse(null);
            if (owner == null) return ResponseEntity.status(404).body("ไม่พบผู้ใช้ id: " + ownerId);

            Warehouse warehouse = new Warehouse();
            warehouse.setWarehouseId("W" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
            warehouse.setWarehouseName(whName);
            warehouse.setWarehouseDetail(whDetail);
            warehouse.setCreatedAt(LocalDateTime.now());
            warehouse.setOwner(owner);

            Warehouse saved = warehouseRepository.save(warehouse);

            // เมื่อสร้างโกดัง ให้ตัวเองเป็นเจ้าของ (Role O)
            Membership.MembershipId membershipId = new Membership.MembershipId(ownerId, saved.getWarehouseId());
            Membership membership = new Membership();
            membership.setId(membershipId);
            membership.setUser(owner);
            membership.setWarehouse(saved);
            membership.setRole(Membership.Role.O);
            membershipRepository.save(membership);

            Map<String, Object> result = new HashMap<>();
            result.put("warehouse_id",      saved.getWarehouseId());
            result.put("warehouse_name",    saved.getWarehouseName());
            result.put("warehouses_detail", saved.getWarehouseDetail() != null ? saved.getWarehouseDetail() : "");
            result.put("create_at",         saved.getCreatedAt().toString());
            result.put("owner_id",          saved.getOwner().getUserId());
            result.put("owner_username",    saved.getOwner().getUsername());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("สร้างไม่สำเร็จ: " + e.getMessage());
        }
    }

    // GET /api/warehouses/invitations?userId=...
    @GetMapping("/warehouses/invitations")
    public ResponseEntity<?> getInvitations(@RequestParam String userId) {
        try {
            List<Membership> memberships = membershipRepository.findByUserUserIdAndRole(userId, Membership.Role.W);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Membership m : memberships) {
                Map<String, Object> map = new HashMap<>();
                map.put("inv_id",         m.getId().getWarehouseId()); // ใช้อันนี้เพื่อการลบ/อัปเดต
                map.put("warehouse_id",   m.getWarehouse().getWarehouseId());
                map.put("warehouse_name", m.getWarehouse().getWarehouseName());
                map.put("owner_username", m.getWarehouse().getOwner().getUsername());
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // POST /api/warehouses/invitations/{invId}/accept?userId=...
    @PostMapping("/warehouses/invitations/{invId}/accept")
    public ResponseEntity<?> acceptInvitation(
            @PathVariable String invId,
            @RequestParam String userId) {
        try {
            Membership.MembershipId id = new Membership.MembershipId(userId, invId);
            Membership membership = membershipRepository.findById(id).orElse(null);
            if (membership == null) return ResponseEntity.status(404).body("ไม่พบคำเชิญ");

            // เปลี่ยนสถานะเป็น V (Viewer) หรือตำแหน่งอื่นตามที่ต้องการ
            membership.setRole(Membership.Role.V);
            membershipRepository.save(membership);
            return ResponseEntity.ok("ตอบรับสำเร็จ");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // DELETE /api/warehouses/invitations/{invId}?userId=...
    @DeleteMapping("/warehouses/invitations/{invId}")
    public ResponseEntity<?> declineInvitation(
            @PathVariable String invId,
            @RequestParam String userId) {
        try {
            Membership.MembershipId id = new Membership.MembershipId(userId, invId);
            if (!membershipRepository.existsById(id)) return ResponseEntity.status(404).body("ไม่พบคำเชิญ");

            membershipRepository.deleteById(id);
            return ResponseEntity.ok("ปฏิเสธสำเร็จ");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // POST /api/memberships
    @PostMapping("/memberships")
    public ResponseEntity<?> addMember(@RequestBody Map<String, String> body) {
        try {
            String targetUserId = body.get("user_id");
            String warehouseId  = body.get("warehouse_id");
            String roleStr      = body.get("role");

            if (roleStr.equals("O")) {
                return ResponseEntity.status(400).body("ไม่สามารถกำหนด Role O ได้");
            }

            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null) return ResponseEntity.status(404).body("ไม่พบผู้ใช้");

            Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            if (warehouse == null) return ResponseEntity.status(404).body("ไม่พบโกดัง");

            Membership.MembershipId newId = new Membership.MembershipId(targetUserId, warehouseId);
            if (membershipRepository.existsById(newId)) {
                return ResponseEntity.status(409).body("สมาชิกคนนี้มีอยู่แล้วในโกดัง");
            }

            Membership membership = new Membership();
            membership.setId(newId);
            membership.setUser(targetUser);
            membership.setWarehouse(warehouse);
            membership.setRole(Membership.Role.valueOf(roleStr));
            membershipRepository.save(membership);

            return ResponseEntity.ok("เพิ่มสมาชิกสำเร็จ");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // PUT /api/memberships/{userId}/{warehouseId}/role
    @PutMapping("/memberships/{userId}/{warehouseId}/role")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable("userId") String targetUserId,
            @PathVariable("warehouseId") String warehouseId,
            @RequestParam("role") String roleStr) {
        try {
            if (roleStr.equals("O")) return ResponseEntity.status(400).body("ไม่สามารถกำหนด Role O ได้");

            Membership.MembershipId targetKey = new Membership.MembershipId(targetUserId, warehouseId);
            Membership targetMembership = membershipRepository.findById(targetKey).orElse(null);
            if (targetMembership == null) return ResponseEntity.status(404).body("ไม่พบสมาชิกคนนี้ในโกดัง");

            targetMembership.setRole(Membership.Role.valueOf(roleStr));
            membershipRepository.save(targetMembership);

            return ResponseEntity.ok("เปลี่ยน role สำเร็จ");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    @GetMapping("/warehouses/{warehouseId}/products")
    public ResponseEntity<?> getProductsByWarehouse(@PathVariable String warehouseId) {
        try {
            Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            if (warehouse == null) return ResponseEntity.status(404).body("ไม่พบโกดัง");

            List<Map<String, Object>> result = new ArrayList<>();
            if (warehouse.getProducts() != null) {
                for (var product : warehouse.getProducts()) {

                    // 🌟 เติม If บรรทัดนี้: ถ้าสินค้าขึ้นสถานะว่าโดนลบแล้ว (isDeleted = true) ให้ข้ามชิ้นนี้ไปเลย ไม่โชว์
                    if (product.isDeleted()) {
                        continue;
                    }

                    Map<String, Object> map = new HashMap<>();
                    map.put("product_id",    product.getProductId());
                    map.put("product_name",  product.getProductName() != null ? product.getProductName() : "");
                    map.put("category_name", product.getCategory() != null ? product.getCategory().getCategoryName() : null);
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

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // DELETE /api/warehouses/{warehouseId}
    @Transactional
    @DeleteMapping("/warehouses/{warehouseId}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable String warehouseId) {
        try {
            if (!warehouseRepository.existsById(warehouseId)) return ResponseEntity.status(404).body("ไม่พบโกดัง");
            warehouseRepository.deleteById(warehouseId);
            return ResponseEntity.ok("ลบสำเร็จ");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("ลบไม่สำเร็จ: " + e.getMessage());
        }
    }

    // PUT /api/warehouses/update/{id}
    @PutMapping("/warehouses/update/{id}")
    public ResponseEntity<?> updateWarehouse(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            Warehouse warehouse = warehouseRepository.findById(id).orElse(null);
            if (warehouse == null) {
                return ResponseEntity.status(404).body("ไม่พบโกดัง id: " + id);
            }

            String whName = body.get("warehouse_name");
            String whDetail = body.get("warehouses_detail");

            if (whName != null && !whName.isEmpty()) {
                warehouse.setWarehouseName(whName);
            }
            if (whDetail != null) {
                warehouse.setWarehouseDetail(whDetail);
            }

            Warehouse updatedWarehouse = warehouseRepository.save(warehouse);

            Map<String, Object> result = new HashMap<>();
            result.put("warehouse_id", updatedWarehouse.getWarehouseId());
            result.put("warehouse_name", updatedWarehouse.getWarehouseName());
            result.put("warehouses_detail", updatedWarehouse.getWarehouseDetail() != null ? updatedWarehouse.getWarehouseDetail() : "");
            result.put("create_at", updatedWarehouse.getCreatedAt() != null ? updatedWarehouse.getCreatedAt().toString() : "");
            result.put("owner_id", updatedWarehouse.getOwner() != null ? updatedWarehouse.getOwner().getUserId() : "");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("แก้ไขไม่สำเร็จ: " + e.getMessage());
        }
    }
    // =========================================================
    // 🌟 API ดึงสิทธิ์ของผู้ใช้ในโกดัง (Role: O, E, V)
    // =========================================================
    @GetMapping("/warehouses/{warehouseId}/role")
    public ResponseEntity<?> getUserRoleInWarehouse(@PathVariable String warehouseId, @RequestParam String userId) {
        try {
            Map<String, String> response = new HashMap<>();

            // 1. เช็คก่อนว่าเป็นเจ้าของโกดังหรือไม่ (Owner)
            Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            if (warehouse != null && warehouse.getOwner().getUserId().equals(userId)) {
                response.put("role", "O"); // ถ้าเป็นคนสร้างโกดัง ให้สิทธิ์ Owner (O) ทันที
                return ResponseEntity.ok(response);
            }

            // 2. ถ้าไม่ใช่คนสร้าง ให้ไปค้นหาในตาราง memberships ด้วยฟังก์ชันที่เราเพิ่งสร้าง
            Membership membership = membershipRepository.findByUserUserIdAndWarehouseWarehouseId(userId, warehouseId);

            if (membership != null && membership.getRole() != null) {
                // ดึงสิทธิ์ออกมา (E หรือ V)
                response.put("role", membership.getRole().name());
            } else {
                // ถ้าหาไม่เจอ หรือยังไม่ได้เป็นสมาชิก ให้สิทธิ์ต่ำสุดคือ Viewer (V) ไว้ก่อน
                response.put("role", "V");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // =========================================================
    // 🌟 API ดึงประวัติเข้า-ออก เฉพาะของโกดังนั้นๆ (สำหรับหน้าประวัติโกดัง)
    // =========================================================
    @GetMapping("/history/warehouse/{warehouseId}")
    public ResponseEntity<List<Map<String, Object>>> getWarehouseHistory(@PathVariable String warehouseId) {
        try {
            // ดึงประวัติเรียงจากใหม่ไปเก่า
            List<StockHistory> historyList = stockHistoryRepository.findByWarehouse_WarehouseIdOrderByCreatedAtDesc(warehouseId);

            List<Map<String, Object>> result = new ArrayList<>();
            for(StockHistory h : historyList) {
                Map<String, Object> map = new HashMap<>();
                map.put("historyId", h.getStockHistoryId());
                map.put("amount", h.getAmount());
                map.put("type", h.getType().name()); // IN หรือ OUT
                map.put("date", h.getCreatedAt() != null ? h.getCreatedAt().toString() : "");

                // ดึงชื่อสินค้าและหมวดหมู่ (ดัก null ไว้กันแอปพัง)
                if (h.getProduct() != null) {
                    map.put("productName", h.getProduct().getProductName());
                    map.put("categoryName", h.getProduct().getCategory() != null ? h.getProduct().getCategory().getCategoryName() : "ไม่ระบุ");
                } else {
                    map.put("productName", "สินค้าถูกลบ");
                    map.put("categoryName", "-");
                }

                // ดึงชื่อคนทำรายการ
                map.put("username", h.getUser() != null ? h.getUser().getUsername() : "ไม่ระบุ");

                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}