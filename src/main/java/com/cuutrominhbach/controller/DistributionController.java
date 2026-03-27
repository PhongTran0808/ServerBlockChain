package com.cuutrominhbach.controller;

import com.cuutrominhbach.dto.request.CreateDistributionRoundRequest;
import com.cuutrominhbach.dto.response.ClaimableDistributionResponse;
import com.cuutrominhbach.dto.response.DistributionRoundResponse;
import com.cuutrominhbach.entity.DistributionRound;
import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.UserRepository;
import com.cuutrominhbach.service.DistributionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/distributions")
public class DistributionController {

    private final DistributionService distributionService;
    private final UserRepository userRepository;

    public DistributionController(DistributionService distributionService, UserRepository userRepository) {
        this.distributionService = distributionService;
        this.userRepository = userRepository;
    }

    @PostMapping("/rounds")
    public ResponseEntity<DistributionRoundResponse> createRound(@RequestBody CreateDistributionRoundRequest request) {
        User me = currentUser();
        if (me.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ ADMIN mới có quyền tạo vòng phân phối");
        }

        DistributionRound round = distributionService.createRound(request);
        return ResponseEntity.ok(DistributionRoundResponse.from(round));
    }

    @GetMapping("/rounds")
    public ResponseEntity<List<DistributionRoundResponse>> getRounds(@RequestParam(required = false) String province) {
        User me = currentUser();
        if (me.getRole() == Role.CITIZEN) {
            province = me.getProvince();
        }

        List<DistributionRoundResponse> rounds = distributionService.getRounds(province)
                .stream().map(DistributionRoundResponse::from).toList();
        return ResponseEntity.ok(rounds);
    }

    @GetMapping("/claimable")
    public ResponseEntity<List<ClaimableDistributionResponse>> getClaimable() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(distributionService.getClaimableRounds(userId));
    }

    @PostMapping("/rounds/{roundId}/claim")
    public ResponseEntity<Map<String, Object>> claim(@PathVariable Long roundId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(distributionService.claim(userId, roundId));
    }

    private User currentUser() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
