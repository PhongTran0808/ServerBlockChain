package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.BatchItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchItemRepository extends JpaRepository<BatchItem, Long> {
    List<BatchItem> findByBatchId(Long batchId);
    void deleteByBatchId(Long batchId);
}
