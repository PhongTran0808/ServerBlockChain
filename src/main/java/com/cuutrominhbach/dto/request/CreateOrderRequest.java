package com.cuutrominhbach.dto.request;

public class CreateOrderRequest {
    private Long itemId;
    private Long shopId;
    private String pin;
    /**
     * Giá shop thực tế — phải <= item.priceTokens (giá trần).
     * Nếu không truyền, mặc định bằng giá trần (không có chênh lệch).
     */
    private Long shopPrice;

    public CreateOrderRequest() {}

    public Long getItemId() { return itemId; }
    public Long getShopId() { return shopId; }
    public String getPin() { return pin; }
    public Long getShopPrice() { return shopPrice; }

    public void setItemId(Long itemId) { this.itemId = itemId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public void setPin(String pin) { this.pin = pin; }
    public void setShopPrice(Long shopPrice) { this.shopPrice = shopPrice; }
}
