package com.cuutrominhbach.dto.request;

public class CreateOrderRequest {
    private Long itemId;
    private Long shopId;
    private String pin;

    public CreateOrderRequest() {}

    public Long getItemId() { return itemId; }
    public Long getShopId() { return shopId; }
    public String getPin() { return pin; }

    public void setItemId(Long itemId) { this.itemId = itemId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public void setPin(String pin) { this.pin = pin; }
}
