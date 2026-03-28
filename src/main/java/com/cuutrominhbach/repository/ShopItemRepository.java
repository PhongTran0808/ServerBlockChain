package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.ShopItem;
import com.cuutrominhbach.entity.ShopItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
    List<ShopItem> findByShopId(Long shopId);
    List<ShopItem> findByShopIdAndStatus(Long shopId, ShopItemStatus status);
    boolean existsByShopIdAndItemId(Long shopId, Long itemId);
}
