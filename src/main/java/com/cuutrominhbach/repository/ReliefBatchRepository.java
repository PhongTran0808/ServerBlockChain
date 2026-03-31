package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.ReliefBatch;
import com.cuutrominhbach.entity.ReliefBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReliefBatchRepository extends JpaRepository<ReliefBatch, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.province = :province")
    List<ReliefBatch> findByProvince(String province);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.status = :status")
    List<ReliefBatch> findByStatus(com.cuutrominhbach.entity.ReliefBatchStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.province = :province AND b.status = :status")
    List<ReliefBatch> findByProvinceAndStatus(String province, com.cuutrominhbach.entity.ReliefBatchStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.transporter.id = :transporterId")
    List<ReliefBatch> findByTransporterId(Long transporterId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.shop.id = :shopId")
    List<ReliefBatch> findByShopId(Long shopId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter WHERE b.shop.id = :shopId AND b.status = :status")
    List<ReliefBatch> findByShopIdAndStatus(Long shopId, com.cuutrominhbach.entity.ReliefBatchStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b FROM ReliefBatch b LEFT JOIN FETCH b.batchItems bi LEFT JOIN FETCH bi.item LEFT JOIN FETCH b.shop LEFT JOIN FETCH b.transporter")
    List<ReliefBatch> findAllWithItems();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT b FROM ReliefBatch b WHERE b.id = :id")
    java.util.Optional<ReliefBatch> findWithLockById(Long id);
}
