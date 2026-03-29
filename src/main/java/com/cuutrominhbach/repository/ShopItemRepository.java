package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.ShopItem;
import com.cuutrominhbach.entity.ShopItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT si FROM ShopItem si JOIN FETCH si.item WHERE si.shop.id = :shopId")
    List<ShopItem> findByShopId(Long shopId);

    @org.springframework.data.jpa.repository.Query("SELECT si FROM ShopItem si JOIN FETCH si.item WHERE si.shop.id = :shopId AND si.status = :status")
    List<ShopItem> findByShopIdAndStatus(Long shopId, ShopItemStatus status);
    boolean existsByShopIdAndItemId(Long shopId, Long itemId);
}
