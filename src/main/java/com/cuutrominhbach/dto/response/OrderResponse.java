package com.cuutrominhbach.dto.response;

import com.cuutrominhbach.entity.Order;
import com.cuutrominhbach.entity.OrderStatus;

import java.time.LocalDateTime;

public class OrderResponse {
    private Long id;
    private Long citizenId;
    private String citizenName;
    private Long shopId;
    private String shopName;
    private Long transporterId;
    private String transporterName;
    private OrderStatus status;
    private Long totalTokens;
    private Long shopPrice;
    private Long refundAmount;
    private String lockTxHash;
    private String releaseTxHash;
    private String spreadRefundTxHash;
    private Long itemId;
    private Boolean isFlagged;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        OrderResponse r = new OrderResponse();
        r.id = order.getId();
        if (order.getCitizen() != null) {
            r.citizenId = order.getCitizen().getId();
            r.citizenName = order.getCitizen().getFullName();
        }
        if (order.getShop() != null) {
            r.shopId = order.getShop().getId();
            r.shopName = order.getShop().getFullName();
        }
        if (order.getTransporter() != null) {
            r.transporterId = order.getTransporter().getId();
            r.transporterName = order.getTransporter().getFullName();
        }
        r.status = order.getStatus();
        r.totalTokens = order.getTotalTokens();
        r.shopPrice = order.getShopPrice();
        r.refundAmount = order.getRefundAmount();
        r.lockTxHash = order.getLockTxHash();
        r.releaseTxHash = order.getReleaseTxHash();
        r.spreadRefundTxHash = order.getSpreadRefundTxHash();
        r.itemId = order.getItemId();
        r.isFlagged = order.getIsFlagged();
        r.createdAt = order.getCreatedAt();
        r.updatedAt = order.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getCitizenId() { return citizenId; }
    public String getCitizenName() { return citizenName; }
    public Long getShopId() { return shopId; }
    public String getShopName() { return shopName; }
    public Long getTransporterId() { return transporterId; }
    public String getTransporterName() { return transporterName; }
    public OrderStatus getStatus() { return status; }
    public Long getTotalTokens() { return totalTokens; }
    public Long getShopPrice() { return shopPrice; }
    public Long getRefundAmount() { return refundAmount; }
    public String getLockTxHash() { return lockTxHash; }
    public String getReleaseTxHash() { return releaseTxHash; }
    public String getSpreadRefundTxHash() { return spreadRefundTxHash; }
    public Long getItemId() { return itemId; }
    public Boolean getIsFlagged() { return isFlagged; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
