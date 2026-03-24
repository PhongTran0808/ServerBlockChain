package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.CampaignPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignPoolRepository extends JpaRepository<CampaignPool, Long> {

    Optional<CampaignPool> findByProvince(String province);

    List<CampaignPool> findAll();
}
