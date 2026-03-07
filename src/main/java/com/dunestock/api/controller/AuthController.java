package com.dunestock.api.controller;

import com.dunestock.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.dunestock.api.model.User;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // สำคัญ: เพื่อให้ Android ยิงเข้าเครื่องคอมได้
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if(userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username นี้ถูกใช้ไปแล้ว");
        }
        user.setUserId("U" + UUID.randomUUID().toString().substring(0, 4));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        Optional<User> user = userRepository.findByEmail(email);

        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "ไม่พบบัญชีนี้"));
        }

        if (!passwordEncoder.matches(password, user.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "รหัสผ่านไม่ถูกต้อง"));
        }

        return ResponseEntity.ok(user.get());
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String,String> body) {
        String userId = body.get("userId");
        String username = body.get("username");
        String email = body.get("email");

        User user  = userRepository.findByUserId(userId).get();

        boolean usernameTaken = userRepository.findByUsername(username)
                .filter(u -> !u.getUserId().equals(userId))
                .isPresent();
        if (usernameTaken) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username นี้ถูกใช้ไปแล้ว"));
        }

        boolean emailTaken = userRepository.findByEmail(email)
                .filter(u -> !u.getUserId().equals(userId))
                .isPresent();
        if (emailTaken) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email นี้ถูกใช้ไปแล้ว"));
        }
        user.setUsername(username);
        user.setEmail(email);
        userRepository.save(user);

        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String,String> body) {
        String userId = body.get("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        User user = userRepository.findByUserId(userId).get();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) { // เปลี่ยนตรงนี้
            return ResponseEntity.badRequest().body(Map.of("message", "รหัสผ่านเดิมไม่ถูกต้อง"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "เปลี่ยนรหัสผ่านสำเร็จ"));
    }
}