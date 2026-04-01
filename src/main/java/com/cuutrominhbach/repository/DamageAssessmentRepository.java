package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.DamageAssessment;
import com.cuutrominhbach.entity.DamageAssessmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DamageAssessmentRepository extends JpaRepository<DamageAssessment, Long> {

    List<DamageAssessment> findByStatus(DamageAssessmentStatus status);

    List<DamageAssessment> findByStatusAndCreatedAtBefore(DamageAssessmentStatus status, LocalDateTime timeObj);

}
