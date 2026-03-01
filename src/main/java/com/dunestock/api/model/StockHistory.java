package com.dunestock.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_history")
public class StockHistory {
    @Id
    @Column(name = "stock_history_id", length = 5)
    private String stockHistoryId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('IN', 'OUT')")
    private StockType type;

    private int amount;

    @Column(name = "create_at")
    private LocalDateTime createdAt;

    // 1. เชื่อมกับ Product (ดูว่าสินค้าชิ้นไหน)
    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"warehouse", "category"}) // ไม่ต้องดึงซ้ำซ้อน
    private Product product;

    // 2. เชื่อมกับ Warehouse (จุดที่คุณต้องการเพิ่ม!)
    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    @JsonIgnoreProperties({"products", "categories", "owner"})
    private Warehouse warehouse;

    // 3. เชื่อมกับ User (ดูว่าใครเป็นคนกดยืนยัน)
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore // ปกติประวัติไม่ต้องโชว์ข้อมูล User แบบละเอียด
    private User user;

    public enum StockType {
        IN, OUT
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getStockHistoryId() {
        return stockHistoryId;
    }

    public void setStockHistoryId(String stockHistoryId) {
        this.stockHistoryId = stockHistoryId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public StockType getType() {
        return type;
    }

    public void setType(StockType type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}