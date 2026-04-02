package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.WalletUpdateRequest;
import com.cuutrominhbach.dto.response.UserResponse;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
     * Cập nhật địa chỉ ví blockchain của người dùng hiện tại.
     * Chỉ SHOP hoặc TRANSPORTER mới được phép gọi endpoint này.
     */
    @PutMapping("/me/wallet")
    public ResponseEntity<?> updateMyWallet(@Valid @RequestBody WalletUpdateRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Check role — only SHOP or TRANSPORTER allowed
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAllowed = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SHOP") || a.getAuthority().equals("ROLE_TRANSPORTER"));
        if (!isAllowed) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ SHOP hoặc TRANSPORTER mới được cập nhật địa chỉ ví."));
        }

        String walletLower = request.walletAddress().toLowerCase();

        // Check uniqueness
        if (userRepository.existsByWalletAddressAndIdNot(walletLower, userId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Địa chỉ ví này đã được đăng ký bởi một tài khoản khác."));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        user.setWalletAddress(walletLower);
        userRepository.save(user);

        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Danh sách cửa hàng đã được duyệt — dùng cho TNV chọn shop khi nhận lô.
     * Trả về walletAddress để frontend cảnh báo shop chưa có ví blockchain.
     * Endpoint public, không cần quyền ADMIN.
     */
    @GetMapping("/shops")
    public ResponseEntity<List<Map<String, Object>>> getApprovedShops() {
        List<Map<String, Object>> shops = userRepository
                .findByRoleAndIsApproved(Role.SHOP, true)
                .stream()
                .map(u -> {
                    // Dùng HashMap thay vì Map.of để hỗ trợ giá trị null (walletAddress có thể null)
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", u.getId());
                    m.put("fullName", u.getFullName() != null ? u.getFullName() : u.getUsername());
                    m.put("province", u.getProvince() != null ? u.getProvince() : "");
                    // Trả về walletAddress để frontend lọc shop chưa có ví
                    m.put("walletAddress", u.getWalletAddress() != null ? u.getWalletAddress() : "");
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(shops);
    }
}
