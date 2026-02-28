package com.dunestock.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "user_id", length = 5)
    private String userId;

    @Column(length = 50)
    private String username;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(name = "create_at")
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    @Column(name = "delete_at")
    private LocalDateTime deletedAt;

    // ความสัมพันธ์: User เป็นเจ้าของ Warehouse
    @OneToMany(mappedBy = "owner")
    private List<Warehouse> ownedWarehouses;

    public interface UserRepository extends JpaRepository<User, String> {
        Optional<User> findByUsername(String username);
        Optional<User> findByEmail(String email);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<Warehouse> getOwnedWarehouses() {
        return ownedWarehouses;
    }

    public void setOwnedWarehouses(List<Warehouse> ownedWarehouses) {
        this.ownedWarehouses = ownedWarehouses;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }




    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Membership> memberships; // username
}