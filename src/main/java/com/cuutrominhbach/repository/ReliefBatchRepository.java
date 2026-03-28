package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.ReliefBatch;
import com.cuutrominhbach.entity.ReliefBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReliefBatchRepository extends JpaRepository<ReliefBatch, Long> {
    List<ReliefBatch> findByProvince(String province);
    List<ReliefBatch> findByStatus(ReliefBatchStatus status);
    List<ReliefBatch> findByProvinceAndStatus(String province, ReliefBatchStatus status);
    List<ReliefBatch> findByTransporterId(Long transporterId);
    List<ReliefBatch> findByShopId(Long shopId);
    List<ReliefBatch> findByShopIdAndStatus(Long shopId, ReliefBatchStatus status);
}
