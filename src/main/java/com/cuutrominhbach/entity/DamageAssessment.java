package com.cuutrominhbach.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "damage_assessments")
public class DamageAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private User transporter;

    @Column(name = "damage_level", nullable = false)
    private Integer damageLevel;

    @Column(name = "evidence_image_url")
    private String evidenceImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DamageAssessmentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public DamageAssessment() {
    }

    public DamageAssessment(User citizen, User transporter, Integer damageLevel, String evidenceImageUrl, DamageAssessmentStatus status, LocalDateTime createdAt) {
        this.citizen = citizen;
        this.transporter = transporter;
        this.damageLevel = damageLevel;
        this.evidenceImageUrl = evidenceImageUrl;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCitizen() {
        return citizen;
    }

    public void setCitizen(User citizen) {
        this.citizen = citizen;
    }

    public User getTransporter() {
        return transporter;
    }

    public void setTransporter(User transporter) {
        this.transporter = transporter;
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
