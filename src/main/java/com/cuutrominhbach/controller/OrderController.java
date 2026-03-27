package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.ConfirmDeliveryRequest;
import com.cuutrominhbach.dto.request.CreateOrderRequest;
import com.cuutrominhbach.dto.request.OfflineQueueItem;
import com.cuutrominhbach.dto.response.OrderResponse;
import com.cuutrominhbach.security.JwtTokenProvider;
import com.cuutrominhbach.service.EscrowService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final EscrowService escrowService;
    private final JwtTokenProvider jwtTokenProvider;

    public OrderController(EscrowService escrowService, JwtTokenProvider jwtTokenProvider) {
        this.escrowService = escrowService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest req,
                                                      HttpServletRequest request) {
        Long citizenId = getUserId(request);
        return ResponseEntity.ok(escrowService.createOrder(citizenId, req));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getOrders(HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long userId = Long.valueOf(claims.get("userId").toString());
        String role = claims.get("role").toString();

        List<OrderResponse> orders = switch (role) {
            case "CITIZEN" -> escrowService.getOrdersByCitizen(userId);
            case "SHOP" -> escrowService.getOrdersByShop(userId);
            case "TRANSPORTER" -> escrowService.getOrdersByTransporter(userId);
            default -> escrowService.getAllOrders();
        };
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/orders/{id}/ready")
    public ResponseEntity<OrderResponse> markReady(@PathVariable Long id,
                                                   HttpServletRequest request) {
        Long shopId = getUserId(request);
        return ResponseEntity.ok(escrowService.markReady(id, shopId));
    }

    @PutMapping("/orders/{id}/pickup")
    public ResponseEntity<OrderResponse> markPickup(@PathVariable Long id,
                                                     HttpServletRequest request) {
        Long transporterId = getUserId(request);
        return ResponseEntity.ok(escrowService.markPickup(id, transporterId));
    }

    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<OrderResponse> confirmDelivery(@PathVariable Long id,
                                                          @RequestBody ConfirmDeliveryRequest req) {
        return ResponseEntity.ok(escrowService.confirmDelivery(id, req));
    }

    @PostMapping("/sync/offline-queue")
    public ResponseEntity<List<OrderResponse>> syncOfflineQueue(@RequestBody List<OfflineQueueItem> items,
                                                                  HttpServletRequest request) {
        Long transporterId = getUserId(request);
        return ResponseEntity.ok(escrowService.syncOfflineQueue(transporterId, items));
    }

    private Long getUserId(HttpServletRequest request) {
        Claims claims = getClaims(request);
        return Long.valueOf(claims.get("userId").toString());
    }

    private Claims getClaims(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header.substring(7);
        return jwtTokenProvider.parseToken(token);
    }
}
