package com.cuutrominhbach.dto.response;

import com.cuutrominhbach.entity.ShopItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShopItemResponse {
    private Long id;
    private Long shopId;
    private Long itemId;
    private String itemName;
    private String itemImageUrl;
    private Long itemCeilingPrice;   // giá trần từ bảng items
    private BigDecimal shopPrice;
    private Integer quantity;
    private String status;
    private LocalDateTime updatedAt;

    public static ShopItemResponse from(ShopItem si) {
        ShopItemResponse r = new ShopItemResponse();
        r.id = si.getId();
        r.shopId = si.getShop() != null ? si.getShop().getId() : null;
        if (si.getItem() != null) {
            r.itemId = si.getItem().getId();
            r.itemName = si.getItem().getName();
            r.itemImageUrl = si.getItem().getImageUrl();
            r.itemCeilingPrice = si.getItem().getPriceTokens();
        }
        r.shopPrice = si.getShopPrice();
        r.quantity = si.getQuantity();
        r.status = si.getStatus() != null ? si.getStatus().name() : null;
        r.updatedAt = si.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getShopId() { return shopId; }
    public Long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getItemImageUrl() { return itemImageUrl; }
    public Long getItemCeilingPrice() { return itemCeilingPrice; }
    public BigDecimal getShopPrice() { return shopPrice; }
    public Integer getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
