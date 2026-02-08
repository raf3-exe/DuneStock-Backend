package com.dunestock.api.model;

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
    private StockType type; // ต้องสร้าง Enum แยกหรือใส่ไว้ในไฟล์เดียวกัน

    private int amount;

    @Column(name = "create_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public enum StockType {
        IN, OUT
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