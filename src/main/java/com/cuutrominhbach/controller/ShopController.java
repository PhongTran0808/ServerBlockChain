package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.ShopItemRequest;
import com.cuutrominhbach.dto.response.ShopItemResponse;
import com.cuutrominhbach.security.JwtTokenProvider;
import com.cuutrominhbach.service.ShopItemService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopItemService shopItemService;
    private final JwtTokenProvider jwtTokenProvider;

    public ShopController(ShopItemService shopItemService, JwtTokenProvider jwtTokenProvider) {
        this.shopItemService = shopItemService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** GET /api/shop/inventory — Lấy kho hàng của shop đang đăng nhập */
    @GetMapping("/inventory")
    public ResponseEntity<List<ShopItemResponse>> getInventory(HttpServletRequest request) {
        Long shopId = getUserId(request);
        return ResponseEntity.ok(shopItemService.getInventory(shopId));
    }

    /** POST /api/shop/inventory — Thêm vật phẩm vào kho */
    @PostMapping("/inventory")
    public ResponseEntity<ShopItemResponse> addToInventory(@RequestBody ShopItemRequest req,
                                                            HttpServletRequest request) {
        Long shopId = getUserId(request);
        return ResponseEntity.ok(shopItemService.addToInventory(shopId, req));
    }

    /** PUT /api/shop/inventory/{id} — Cập nhật giá / số lượng / trạng thái */
    @PutMapping("/inventory/{id}")
    public ResponseEntity<ShopItemResponse> updateInventory(@PathVariable Long id,
                                                             @RequestBody ShopItemRequest req,
                                                             HttpServletRequest request) {
        Long shopId = getUserId(request);
        return ResponseEntity.ok(shopItemService.updateInventory(shopId, id, req));
    }

    private Long getUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header.substring(7);
        Claims claims = jwtTokenProvider.parseToken(token);
        return Long.valueOf(claims.get("userId").toString());
    }
}
