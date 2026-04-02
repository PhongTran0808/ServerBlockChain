package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.ReliefBatch;
import com.cuutrominhbach.entity.ReliefBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReliefBatchRepository extends JpaRepository<ReliefBatch, Long> {
    @Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.province = :province")
    List<ReliefBatch> findByProvince(String province);

    @Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.status = :status")
    List<ReliefBatch> findByStatus(ReliefBatchStatus status);

    @Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.province = :province AND b.status = :status")
    List<ReliefBatch> findByProvinceAndStatus(String province, ReliefBatchStatus status);

    @Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.transporter.id = :transporterId")
    List<ReliefBatch> findByTransporterId(Long transporterId);

    @Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.shop.id = :shopId")
    List<ReliefBatch> findByShopId(Long shopId);

    @Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.shop.id = :shopId AND b.status = :status")
    List<ReliefBatch> findByShopIdAndStatus(Long shopId, ReliefBatchStatus status);

    @Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter")
    List<ReliefBatch> findAllWithItems();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ReliefBatch b WHERE b.id = :id")
    Optional<ReliefBatch> findWithLockById(Long id);
}
