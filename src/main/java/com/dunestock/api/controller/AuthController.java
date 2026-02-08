package com.dunestock.api.controller;

import com.dunestock.api.model.User;
import com.dunestock.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // สำคัญ: เพื่อให้ Android ยิงเข้าเครื่องคอมได้
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        // เช็คว่ามี Username ซ้ำไหม
        if(userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username นี้ถูกใช้ไปแล้ว");
        }

        // สุ่ม ID สั้นๆ 5 หลักตาม ER Diagram (VARCHAR(5))
        user.setUserId("U" + UUID.randomUUID().toString().substring(0, 4));
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        // ค้นหาด้วย email ตามหน้าจอสีครีมที่คุณเขียน
        return userRepository.findByEmail(body.get("email"))
                .filter(u -> u.getPassword().equals(body.get("password")))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }
}