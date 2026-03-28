package com.cuutrominhbach.controller;

import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Danh sách cửa hàng đã được duyệt — dùng cho citizen chọn shop khi đặt hàng.
     * Endpoint public, không cần quyền ADMIN.
     */
    @GetMapping("/shops")
    public ResponseEntity<List<Map<String, Object>>> getApprovedShops() {
        List<Map<String, Object>> shops = userRepository
                .findByRoleAndIsApproved(Role.SHOP, true)
                .stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "fullName", u.getFullName() != null ? u.getFullName() : u.getUsername(),
                        "province", u.getProvince() != null ? u.getProvince() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(shops);
    }
}
