package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.response.DamageAssessmentResponse;
import com.cuutrominhbach.security.JwtTokenProvider;
import com.cuutrominhbach.service.DamageAssessmentService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DamageAssessmentController {

    private final DamageAssessmentService damageAssessmentService;
    private final JwtTokenProvider jwtTokenProvider;

    public DamageAssessmentController(DamageAssessmentService damageAssessmentService,
                                      JwtTokenProvider jwtTokenProvider) {
        this.damageAssessmentService = damageAssessmentService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/transporter/assess-damage")
    public ResponseEntity<DamageAssessmentResponse> assessDamage(
            @RequestParam("citizenId") Long citizenId,
            @RequestParam("damageLevel") Integer damageLevel,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {

        Long transporterId = getUserId(request);
        String role = getRole(request);
        if (!"TRANSPORTER".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        DamageAssessmentResponse result = damageAssessmentService.assessDamage(transporterId, citizenId, damageLevel, file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/public/damage-reports")
    public ResponseEntity<List<DamageAssessmentResponse>> getPublicReports() {
        return ResponseEntity.ok(damageAssessmentService.getPublicReports());
    }

    @PostMapping("/public/damage-reports/{id}/report")
    public ResponseEntity<String> reportDispute(@PathVariable Long id, HttpServletRequest request) {
        Long reporterId = getUserId(request);
        String role = getRole(request);
        if (!"CITIZEN".equals(role)) {
            return ResponseEntity.status(403).body("Chỉ người dân mới có thể thực hiện giám sát báo cáo sai phạm.");
        }

        damageAssessmentService.reportDispute(reporterId, id);
        return ResponseEntity.ok("Đã báo cáo hồ sơ sai sự thật thành công!");
    }

    private Long getUserId(HttpServletRequest request) {
        Claims claims = getClaims(request);
        return Long.valueOf(claims.get("userId").toString());
    }

    private String getRole(HttpServletRequest request) {
        Claims claims = getClaims(request);
        return claims.get("role").toString();
    }

    private Claims getClaims(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Thiếu token xác thực");
        }
        String token = header.substring(7);
        return jwtTokenProvider.parseToken(token);
    }
}
