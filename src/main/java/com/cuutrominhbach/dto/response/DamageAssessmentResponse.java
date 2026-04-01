package com.cuutrominhbach.dto.response;

import com.cuutrominhbach.entity.DamageAssessment;
import com.cuutrominhbach.entity.DamageAssessmentStatus;

import java.time.LocalDateTime;

public class DamageAssessmentResponse {
    private Long id;
    private String citizenName;
    private String province;
    private Integer damageLevel;
    private String evidenceImageUrl;
    private DamageAssessmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public DamageAssessmentResponse() {
    }

    public DamageAssessmentResponse(DamageAssessment assessment) {
        this.id = assessment.getId();
        this.citizenName = maskName(assessment.getCitizen().getFullName());
        this.province = assessment.getCitizen().getProvince();
        this.damageLevel = assessment.getDamageLevel();
        this.evidenceImageUrl = assessment.getEvidenceImageUrl();
        this.status = assessment.getStatus();
        this.createdAt = assessment.getCreatedAt();
        this.approvedAt = assessment.getApprovedAt();
    }

    private String maskName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "";
        String[] parts = fullName.split(" ");
        if (parts.length <= 1) return fullName;

        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i == 0 || i == parts.length - 1) {
                masked.append(parts[i]);
            } else {
                masked.append(parts[i].charAt(0)).append("***");
            }
            if (i < parts.length - 1) {
                masked.append(" ");
            }
        }
        return masked.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public Integer getDamageLevel() {
        return damageLevel;
    }

    public void setDamageLevel(Integer damageLevel) {
        this.damageLevel = damageLevel;
    }

    public String getEvidenceImageUrl() {
        return evidenceImageUrl;
    }

    public void setEvidenceImageUrl(String evidenceImageUrl) {
        this.evidenceImageUrl = evidenceImageUrl;
    }

    public DamageAssessmentStatus getStatus() {
        return status;
    }

    public void setStatus(DamageAssessmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
