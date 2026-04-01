package com.cuutrominhbach.service;

import com.cuutrominhbach.dto.response.DamageAssessmentResponse;
import com.cuutrominhbach.entity.DamageAssessment;
import com.cuutrominhbach.entity.DamageAssessmentStatus;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.DamageAssessmentRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DamageAssessmentService {

    private final DamageAssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public DamageAssessmentService(DamageAssessmentRepository assessmentRepository,
                                   UserRepository userRepository,
                                   FileStorageService fileStorageService) {
        this.assessmentRepository = assessmentRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    public DamageAssessmentResponse assessDamage(Long transporterId, Long citizenId, Integer damageLevel, MultipartFile file) {
        User transporter = userRepository.findById(transporterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin TNV!"));

        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dân!"));

        if (damageLevel < 1 || damageLevel > 3) {
            throw new IllegalArgumentException("Mức độ thiệt hại không hợp lệ (1-3)!");
        }

        if ((damageLevel == 2 || damageLevel == 3) && (file == null || file.isEmpty())) {
            throw new IllegalArgumentException("Mức độ 2 và 3 bắt buộc phải có ảnh hiện trường!");
        }

        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = fileStorageService.storeFile(file);
        }

        DamageAssessment assessment = new DamageAssessment(
                citizen,
                transporter,
                damageLevel,
                fileUrl,
                DamageAssessmentStatus.PENDING_3_DAYS,
                LocalDateTime.now()
        );

        assessment = assessmentRepository.save(assessment);
        return new DamageAssessmentResponse(assessment);
    }

    public List<DamageAssessmentResponse> getPublicReports() {
        return assessmentRepository.findByStatus(DamageAssessmentStatus.PENDING_3_DAYS)
                .stream()
                .map(DamageAssessmentResponse::new)
                .collect(Collectors.toList());
    }

    public void reportDispute(Long reporterId, Long assessmentId) {
        userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("Người báo cáo không hợp lệ!"));

        DamageAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ thiệt hại này!"));

        if (assessment.getStatus() != DamageAssessmentStatus.PENDING_3_DAYS) {
            throw new IllegalArgumentException("Hồ sơ không còn trong trạng thái giám sát!");
        }

        // Tùy chọn: Validate reporter cùng khu vực (cùng Tỉnh)
        // if (reporter.getProvince() != null && !reporter.getProvince().equals(assessment.getCitizen().getProvince())) {
        //     throw new IllegalArgumentException("Chỉ người cùng khu vực mới được báo cáo!");
        // }

        assessment.setStatus(DamageAssessmentStatus.DISPUTED);
        assessmentRepository.save(assessment);
    }
}
