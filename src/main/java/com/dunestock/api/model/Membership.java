package com.dunestock.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "memberships")
public class Membership {

    @Transient
    @JsonProperty("username")
    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }

    @EmbeddedId
    @JsonUnwrapped
    private MembershipId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"memberships"})
    private User user;

    @ManyToOne
    @MapsId("warehouseId")
    @JoinColumn(name = "warehouse_id")
    @JsonIgnoreProperties({"categories", "products"})
    private Warehouse warehouse;


    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('O','E','V','W')")
    private Role role;
//    @Enumerated(EnumType.STRING)
//    @Column(name = "role")
//    private Role role;



    // ✅ 1. เพิ่มคอลัมน์ใหม่สำหรับเก็บตำแหน่งที่ตั้งใจจะมอบให้ (แอบทดไว้ก่อน)
    @Enumerated(EnumType.STRING)
    @Column(name = "intended_role", columnDefinition = "ENUM('O','E','V','W')")
    private Role intendedRole;

    // ✅ 2. เพิ่ม Getter / Setter
    public Role getIntendedRole() {
        return intendedRole;
    }

    public void setIntendedRole(Role intendedRole) {
        this.intendedRole = intendedRole;
    }



    // ✅ 1. เพิ่มฟิลด์ Status
    @Column(name = "status")
    private String status = "PENDING"; // PENDING = รอตอบรับ, ACTIVE = ตอบรับแล้ว

    // ✅ 2. อย่าลืมเพิ่ม Getter / Setter ให้ status ด้วยนะครับ
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }



    @Column(name = "create_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Role {
        O, E, V, W
    }

    // ==========================
    // GETTERS & SETTERS
    // ==========================

    public MembershipId getId() {
        return id;
    }

    public void setId(MembershipId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ==========================
    // COMPOSITE KEY
    // ==========================

    @Embeddable
    public static class MembershipId implements Serializable {

        @Column(name = "user_id", length = 255)
        private String userId;

        @Column(name = "warehouse_id", length = 255)
        private String warehouseId;

        public MembershipId() {}

        public MembershipId(String userId, String warehouseId) {
            this.userId = userId;
            this.warehouseId = warehouseId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(String warehouseId) {
            this.warehouseId = warehouseId;
        }
    }


}