package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.DistributionClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistributionClaimRepository extends JpaRepository<DistributionClaim, Long> {
    boolean existsByRoundIdAndCitizenId(Long roundId, Long citizenId);
    List<DistributionClaim> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);
}
