package com.cuutrominhbach.controller;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.dto.request.QrPaymentRequest;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.UserRepository;
import com.cuutrominhbach.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final BlockchainService blockchainService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public PaymentController(BlockchainService blockchainService,
                              UserRepository userRepository,
                              JwtTokenProvider jwtTokenProvider) {
        this.blockchainService = blockchainService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * SHOP quét QR của Citizen để nhận token thanh toán.
     */
    @PostMapping("/qr")
    public ResponseEntity<Map<String, String>> qrPayment(@RequestBody QrPaymentRequest req,
                                                          HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long shopId = Long.valueOf(claims.get("userId").toString());

        User shop = userRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng"));

        if (shop.getRole() != Role.SHOP) {
            throw new IllegalArgumentException("Chỉ SHOP mới được nhận thanh toán QR");
        }

        if (!Boolean.TRUE.equals(shop.getIsApproved())) {
            throw new IllegalArgumentException("Tài khoản cửa hàng chưa được admin phê duyệt");
        }

        String txHash = blockchainService.transferToken(
                req.getCitizenWalletAddress(),
                shop.getWalletAddress(),
                BigInteger.valueOf(req.getTokenId()),
                BigInteger.valueOf(req.getAmount())
        );

        return ResponseEntity.ok(Map.of("txHash", txHash));
    }

    /**
     * SHOP yêu cầu rút tiền VND (ghi nhận yêu cầu).
     */
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, String>> withdraw(@RequestBody Map<String, Object> body,
                                                         HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long shopId = Long.valueOf(claims.get("userId").toString());
        Long amount = Long.valueOf(body.get("amount").toString());

        User shop = userRepository.findById(shopId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng"));

        if (shop.getRole() != Role.SHOP) {
            throw new IllegalArgumentException("Chỉ SHOP mới được gửi yêu cầu rút tiền");
        }

        if (!Boolean.TRUE.equals(shop.getIsApproved())) {
            throw new IllegalArgumentException("Tài khoản cửa hàng chưa được admin phê duyệt");
        }

        // Ghi nhận yêu cầu rút tiền — trong MVP chỉ trả về xác nhận
        return ResponseEntity.ok(Map.of(
                "message", "Yêu cầu rút tiền đã được ghi nhận",
                "shopId", shopId.toString(),
                "amount", amount.toString()
        ));
    }

    private Claims getClaims(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header.substring(7);
        return jwtTokenProvider.parseToken(token);
    }
}
