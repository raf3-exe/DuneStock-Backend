package com.dunestock.api.controller;

import com.dunestock.api.repository.UserRepository;
import com.dunestock.api.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/check-user")
    public ResponseEntity<?> checkUser(@RequestParam("username") String input) {

        Optional<User> userOptional = userRepository.findByUsernameOrEmail(input, input);

        Map<String, Object> response = new HashMap<>();

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            response.put("exists", true);
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());

            return ResponseEntity.ok(response);
        }

        response.put("exists", false);
        response.put("userId", "");
        response.put("username", "");

        return ResponseEntity.ok(response);
    }
}
