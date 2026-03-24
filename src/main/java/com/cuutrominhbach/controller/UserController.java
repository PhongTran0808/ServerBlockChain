package com.cuutrominhbach.controller;

import com.cuutrominhbach.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Lấy tên hiển thị của user theo ID — dùng cho bảng sao kê minh bạch.
     * Chỉ trả về fullName, không lộ thông tin nhạy cảm.
     */
    @GetMapping("/{id}/name")
    public ResponseEntity<Map<String, String>> getUserName(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(Map.of("fullName", u.getFullName() != null ? u.getFullName() : "Ẩn danh")))
                .orElse(ResponseEntity.ok(Map.of("fullName", "Ẩn danh")));
    }
}
