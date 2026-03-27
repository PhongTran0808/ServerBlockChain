package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.DistributionRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistributionRoundRepository extends JpaRepository<DistributionRound, Long> {
    List<DistributionRound> findByProvinceOrderByCreatedAtDesc(String province);
    boolean existsByProvince(String province);
}
