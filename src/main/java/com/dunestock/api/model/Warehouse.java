package com.dunestock.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "warehouses")
public class Warehouse {
    @Id
    @JsonProperty("warehouseId")
    @Column(name = "warehouse_id", length = 5)
    private String warehouseId;

    @Column(name = "warehouse_name", length = 100)
    private String warehouseName;

    @Column(name = "warehouses_detail", length = 255)
    private String warehouseDetail;

    @Column(name = "create_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;

    @OneToMany(mappedBy = "warehouse")
    @JsonIgnoreProperties("warehouse")
    private List<Product> products;

    // เพิ่มตรงนี้: เพื่อให้ Warehouse รู้ว่าตัวเองมีประวัติอะไรบ้าง
    @OneToMany(mappedBy = "warehouse")
    @JsonIgnoreProperties("warehouse")
    private List<StockHistory> stockHistories;

    @OneToMany(mappedBy = "warehouse")
    @JsonIgnoreProperties("warehouse")
    private List<Category> categories;

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseDetail() {
        return warehouseDetail;
    }

    public void setWarehouseDetail(String warehouseDetail) {
        this.warehouseDetail = warehouseDetail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }
}