package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.Item;
import com.cuutrominhbach.entity.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByStatus(ItemStatus status);
}
