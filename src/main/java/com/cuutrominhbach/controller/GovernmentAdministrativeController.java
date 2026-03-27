package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.response.ProvinceOptionResponse;
import com.cuutrominhbach.dto.response.WardOptionResponse;
import com.cuutrominhbach.service.GovernmentAdministrativeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/geo")
public class GovernmentAdministrativeController {

    private final GovernmentAdministrativeService governmentAdministrativeService;

    public GovernmentAdministrativeController(GovernmentAdministrativeService governmentAdministrativeService) {
        this.governmentAdministrativeService = governmentAdministrativeService;
    }

    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces(@RequestParam(required = false) String query) {
        try {
            List<ProvinceOptionResponse> provinces = governmentAdministrativeService.searchProvinces(query);
            return ResponseEntity.ok(Map.of(
                    "source", "government",
                    "count", provinces.size(),
                    "items", provinces
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Lỗi khi truy vấn dữ liệu tỉnh/thành",
                    "message", ex.getMessage(),
                    "details", ex.getClass().getSimpleName()
            ));
        }
    }

    @GetMapping("/wards")
    public ResponseEntity<?> getWards(
            @RequestParam(required = true) String provinceCode,
            @RequestParam(required = false) String query) {
        try {
            List<WardOptionResponse> wards = governmentAdministrativeService.searchWards(provinceCode, query);
            return ResponseEntity.ok(Map.of(
                    "source", "government",
                    "provinceCode", provinceCode,
                    "count", wards.size(),
                    "items", wards
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Lỗi khi truy vấn dữ liệu xã/phường",
                    "message", ex.getMessage(),
                    "details", ex.getClass().getSimpleName()
            ));
        }
    }
}
