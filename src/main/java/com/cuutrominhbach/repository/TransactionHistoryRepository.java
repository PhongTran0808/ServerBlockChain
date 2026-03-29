package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    @Query("SELECT t FROM TransactionHistory t WHERE t.fromUserId = :userId OR t.toUserId = :userId ORDER BY t.createdAt DESC")
    List<TransactionHistory> findByUserId(@Param("userId") Long userId);

    List<TransactionHistory> findByToUserIdOrderByCreatedAtDesc(Long toUserId);

    List<TransactionHistory> findByBatchIdOrderByCreatedAtDesc(Long batchId);

    /** Kiểm tra hiệu quả: citizen đã nhận hàng từ lô này chưa? */
    boolean existsByBatchIdAndTypeAndToUserId(
            Long batchId,
            TransactionHistory.TxType type,
            Long toUserId);
}
