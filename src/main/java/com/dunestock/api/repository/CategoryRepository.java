package com.dunestock.api.repository;

import com.dunestock.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    // แค่นี้เลยครับ ปล่อยว่างๆ ไว้ข้างในได้เลย Spring Boot จะจัดการต่อให้เอง!
}