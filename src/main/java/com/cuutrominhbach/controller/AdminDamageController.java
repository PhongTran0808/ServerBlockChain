package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.response.DamageAssessmentResponse;
import com.cuutrominhbach.service.DamageAssessmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/damage-reports")
public class AdminDamageController {

    private final DamageAssessmentService damageAssessmentService;

    public AdminDamageController(DamageAssessmentService damageAssessmentService) {
        this.damageAssessmentService = damageAssessmentService;
    }

    @GetMapping("/disputed")
    public ResponseEntity<List<DamageAssessmentResponse>> getDisputedReports() {
        return ResponseEntity.ok(damageAssessmentService.getDisputedReports());
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<String> resolveDispute(@PathVariable Long id, @RequestParam("acceptReport") boolean acceptReport) {
        damageAssessmentService.resolveDispute(id, acceptReport);
        String msg = acceptReport ? "Đã CHẤP NHẬN cờ báo cáo -> Hủy bỏ hồ sơ nhận hỗ trợ." 
                                  : "Đã BÁC BỎ cờ báo cáo -> Khôi phục trạng thái chờ giải ngân.";
        return ResponseEntity.ok(msg);
    }
}
