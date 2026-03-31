package com.cuutrominhbach.service;

import com.cuutrominhbach.dto.response.ProvinceOptionResponse;
import com.cuutrominhbach.dto.response.WardOptionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GovernmentAdministrativeService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${geo.government.source-url:https://danhmuchanhchinh.gso.gov.vn/api/v1/provinces}")
    private String governmentSourceUrl;

    @Value("${geo.government.wards-url:https://danhmuchanhchinh.gso.gov.vn/api/v1/wards}")
    private String governmentWardsUrl;

    @Value("${geo.government.cache-minutes:360}")
    private long cacheMinutes;

    private volatile List<ProvinceOptionResponse> cachedProvinces = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public GovernmentAdministrativeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<ProvinceOptionResponse> searchProvinces(String query) {
        List<ProvinceOptionResponse> provinces = getAllProvinces();
        System.err.println("[GEO] Total provinces available: " + provinces.size());
        
        if (query == null || query.isBlank()) {
            System.err.println("[GEO] Query is blank, returning all " + provinces.size() + " provinces");
            return provinces;
        }

        String q = normalize(query);
        System.err.println("[GEO] Searching for normalized query: '" + q + "'");
        
        List<ProvinceOptionResponse> result = provinces.stream()
                .filter(p -> {
                    String nameNorm = normalize(p.name());
                    String codeNorm = normalize(p.code());
                    boolean match = nameNorm.contains(q) || codeNorm.contains(q);
                    if (match) {
                        System.err.println("[GEO] ✓ Matched: " + p.name());
                    }
                    return match;
                })
                .toList();
        
        System.err.println("[GEO] Search result: " + result.size() + " matches");
        return result;
    }

    public boolean isValidProvince(String provinceName) {
        if (provinceName == null || provinceName.isBlank()) {
            return false;
        }
        String expected = normalize(provinceName);
        return getAllProvinces().stream().anyMatch(p -> normalize(p.name()).equals(expected));
    }

    public List<ProvinceOptionResponse> getAllProvinces() {
        Instant now = Instant.now();
        if (!cachedProvinces.isEmpty() && Duration.between(cachedAt, now).toMinutes() < cacheMinutes) {
            System.err.println("[GEO] Returning cached provinces: " + cachedProvinces.size());
            return cachedProvinces;
        }

        synchronized (this) {
            now = Instant.now();
            if (!cachedProvinces.isEmpty() && Duration.between(cachedAt, now).toMinutes() < cacheMinutes) {
                System.err.println("[GEO] Returning cached provinces (double-check): " + cachedProvinces.size());
                return cachedProvinces;
            }

            System.err.println("[GEO] Cache miss, fetching provinces...");
            List<ProvinceOptionResponse> fresh;
            try {
                fresh = fetchFromGovernmentSource();
                System.err.println("[GEO] Fetched from government: " + fresh.size() + " provinces");
            } catch (Exception ex) {
                System.err.println("[GEO] Government fetch failed: " + ex.getMessage());
                fresh = List.of();
            }
            
            if (fresh.isEmpty()) {
                System.err.println("[GEO] Using fallback default provinces");
                fresh = getDefaultProvinces();
            }

            cachedProvinces = fresh.stream()
                    .filter(p -> p.name() != null && !p.name().isBlank())
                    .sorted(Comparator.comparing(ProvinceOptionResponse::name))
                    .toList();
            cachedAt = now;
            System.err.println("[GEO] Cache updated with " + cachedProvinces.size() + " provinces");
            return cachedProvinces;
        }
    }

    private List<ProvinceOptionResponse> getDefaultProvinces() {
        // 34 tỉnh/thành theo dự kiến sáp nhập của chính phủ - Đã thống nhất tên gọi
        return List.of(
                new ProvinceOptionResponse("01", "Hà Nội"),
                new ProvinceOptionResponse("02", "Tuyên Quang"),
                new ProvinceOptionResponse("03", "Lào Cai"),
                new ProvinceOptionResponse("04", "Thái Nguyên"),
                new ProvinceOptionResponse("05", "Phú Thọ"),
                new ProvinceOptionResponse("06", "Bắc Ninh"),
                new ProvinceOptionResponse("07", "Hưng Yên"),
                new ProvinceOptionResponse("08", "Hải Phòng"),
                new ProvinceOptionResponse("09", "Ninh Bình"),
                new ProvinceOptionResponse("10", "Thanh Hóa"),
                new ProvinceOptionResponse("11", "Nghệ An"),
                new ProvinceOptionResponse("12", "Hà Tĩnh"),
                new ProvinceOptionResponse("13", "Quảng Trị"),
                new ProvinceOptionResponse("14", "Đà Nẵng"),
                new ProvinceOptionResponse("15", "Quảng Ngãi"),
                new ProvinceOptionResponse("16", "Gia Lai"),
                new ProvinceOptionResponse("17", "Khánh Hòa"),
                new ProvinceOptionResponse("18", "Lâm Đồng"),
                new ProvinceOptionResponse("19", "Đắk Lắk"),
                new ProvinceOptionResponse("20", "TP.HCM"),
                new ProvinceOptionResponse("21", "Đồng Nai"),
                new ProvinceOptionResponse("22", "Tây Ninh"),
                new ProvinceOptionResponse("23", "Cần Thơ"),
                new ProvinceOptionResponse("24", "Vĩnh Long"),
                new ProvinceOptionResponse("25", "Đồng Tháp"),
                new ProvinceOptionResponse("26", "Cà Mau"),
                new ProvinceOptionResponse("27", "An Giang"),
                new ProvinceOptionResponse("28", "Huế"),
                new ProvinceOptionResponse("29", "Lai Châu"),
                new ProvinceOptionResponse("30", "Điện Biên"),
                new ProvinceOptionResponse("31", "Sơn La"),
                new ProvinceOptionResponse("32", "Lạng Sơn"),
                new ProvinceOptionResponse("33", "Quảng Ninh"),
                new ProvinceOptionResponse("34", "Cao Bằng")
        );
    }


    private List<ProvinceOptionResponse> fetchFromGovernmentSource() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(governmentSourceUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Nguồn dữ liệu chính phủ trả mã lỗi: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<JsonNode> items = extractItemNodes(root);
            List<ProvinceOptionResponse> result = new ArrayList<>();
            for (JsonNode item : items) {
                String code = pickText(item, "code", "id", "ma", "provinceCode", "province_id");
                String name = pickText(item, "name", "provinceName", "ten", "full_name", "province_name");
                if (name == null || name.isBlank()) {
                    continue;
                }
                result.add(new ProvinceOptionResponse(code == null ? "" : code, name.trim()));
            }
            return deduplicateByName(result);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Không thể kết nối nguồn dữ liệu chính phủ", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể kết nối nguồn dữ liệu chính phủ", ex);
        }
    }

    private List<ProvinceOptionResponse> deduplicateByName(List<ProvinceOptionResponse> list) {
        List<ProvinceOptionResponse> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ProvinceOptionResponse p : list) {
            String key = normalize(p.name());
            if (seen.add(key)) {
                out.add(p);
            }
        }
        return out;
    }

    private List<JsonNode> extractItemNodes(JsonNode root) {
        if (root == null || root.isNull()) {
            return List.of();
        }

        if (root.isArray()) {
            List<JsonNode> arr = new ArrayList<>();
            root.forEach(arr::add);
            return arr;
        }

        JsonNode dataNode = firstObjectField(root, "data", "results", "items", "content", "payload");
        if (dataNode != null) {
            return extractItemNodes(dataNode);
        }

        return List.of();
    }

    private JsonNode firstObjectField(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key);
            }
        }
        return null;
    }

    private String pickText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    // ===== WARDS (XÃ/PHƯỜNG) =====
    
    private volatile List<WardOptionResponse> cachedWards = List.of();
    private volatile Instant cachedWardsAt = Instant.EPOCH;
    
    public List<WardOptionResponse> searchWards(String provinceCode, String query) {
        List<WardOptionResponse> wards = getWardsByProvince(provinceCode);
        if (query == null || query.isBlank()) {
            return wards;
        }
        
        String q = normalize(query);
        return wards.stream()
                .filter(w -> normalize(w.name()).contains(q) || normalize(w.code()).contains(q))
                .toList();
    }
    
    public List<WardOptionResponse> getWardsByProvince(String provinceCode) {
        Instant now = Instant.now();
        if (!cachedWards.isEmpty() && Duration.between(cachedWardsAt, now).toMinutes() < cacheMinutes) {
            return cachedWards.stream()
                    .filter(w -> w.provinceCode().equals(provinceCode))
                    .toList();
        }
        
        synchronized (this) {
            now = Instant.now();
            if (!cachedWards.isEmpty() && Duration.between(cachedWardsAt, now).toMinutes() < cacheMinutes) {
                return cachedWards.stream()
                        .filter(w -> w.provinceCode().equals(provinceCode))
                        .toList();
            }
            
            List<WardOptionResponse> fresh;
            try {
                fresh = fetchWardsFromGovernmentSource();
            } catch (Exception ex) {
                System.err.println("[GEO] Ward fetch failed: " + ex.getMessage());
                fresh = getDefaultWards();
            }
            
            cachedWards = fresh.stream()
                    .filter(w -> w.name() != null && !w.name().isBlank())
                    .toList();
            cachedWardsAt = now;
            
            return cachedWards.stream()
                    .filter(w -> w.provinceCode().equals(provinceCode))
                    .toList();
        }
    }
    
    private List<WardOptionResponse> fetchWardsFromGovernmentSource() {
        try {
            HttpRequest request = HttpRequest.newBuilder(java.net.URI.create(governmentWardsUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Lỗi từ API xã/phường: " + response.statusCode());
            }
            
            JsonNode root = objectMapper.readTree(response.body());
            List<JsonNode> items = extractItemNodes(root);
            List<WardOptionResponse> result = new ArrayList<>();
            
            for (JsonNode item : items) {
                String code = pickText(item, "code", "id", "ma", "wardCode", "ward_id");
                String name = pickText(item, "name", "wardName", "ten", "full_name", "ward_name");
                String districtCode = pickText(item, "districtCode", "district_id", "huyen", "huyen_id");
                String provinceCode = pickText(item, "provinceCode", "province_id", "tinh", "tinh_id");
                
                if (name == null || name.isBlank()) continue;
                
                result.add(new WardOptionResponse(
                        code == null ? "" : code,
                        name.trim(),
                        districtCode == null ? "" : districtCode,
                        provinceCode == null ? "" : provinceCode
                ));
            }
            
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Không thể kết nối API xã/phường", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể kết nối API xã/phường", ex);
        }
    }
    
    private List<WardOptionResponse> getDefaultWards() {
        // Fallback: tối thiểu 1 xã cho Hà Nội
        return List.of(
                new WardOptionResponse("01A", "Phường Ba Đình", "", "01"),
                new WardOptionResponse("01B", "Phường Hoàn Kiếm", "", "01"),
                new WardOptionResponse("01C", "Quận Đống Đa", "", "01")
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lowered = value.trim().toLowerCase(Locale.ROOT)
                .replace("đ", "d");
        String noAccent = Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccent;
    }
}
