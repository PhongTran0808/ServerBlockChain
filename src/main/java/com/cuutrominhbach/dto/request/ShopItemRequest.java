package com.cuutrominhbach.dto.request;

import java.math.BigDecimal;

public class ShopItemRequest {
    private Long itemId;
    private BigDecimal shopPrice;
    private Integer quantity;
    private String status; // optional — dùng khi update

    public ShopItemRequest() {}

    public Long getItemId() { return itemId; }
    public BigDecimal getShopPrice() { return shopPrice; }
    public Integer getQuantity() { return quantity; }
    public String getStatus() { return status; }

    public void setItemId(Long itemId) { this.itemId = itemId; }
    public void setShopPrice(BigDecimal shopPrice) { this.shopPrice = shopPrice; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setStatus(String status) { this.status = status; }
}
