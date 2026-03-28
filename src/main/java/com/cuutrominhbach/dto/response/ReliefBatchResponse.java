package com.cuutrominhbach.dto.response;

import com.cuutrominhbach.entity.BatchItem;
import com.cuutrominhbach.entity.ReliefBatch;
import com.cuutrominhbach.entity.ReliefBatchStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ReliefBatchResponse {
    private Long id;
    private String name;
    private String province;
    private Integer totalPackages;
    private Long tokenPerPackage;
    private Integer deliveredCount;
    private ReliefBatchStatus status;
    private Long transporterId;
    private String transporterName;
    private Long shopId;
    private String shopName;
    private Long createdById;
    // Legacy single item
    private Long itemId;
    private String itemName;
    private String itemImageUrl;
    // Multi-item combo
    private List<BatchItemDto> batchItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public record BatchItemDto(Long itemId, String itemName, String itemImageUrl,
                               Long priceTokens, Integer quantity) {}

    public static ReliefBatchResponse from(ReliefBatch b) {
        ReliefBatchResponse r = new ReliefBatchResponse();
        r.id = b.getId();
        r.name = b.getName();
        r.province = b.getProvince();
        r.totalPackages = b.getTotalPackages();
        r.tokenPerPackage = b.getTokenPerPackage();
        r.deliveredCount = b.getDeliveredCount();
        r.status = b.getStatus();
        if (b.getTransporter() != null) {
            r.transporterId = b.getTransporter().getId();
            r.transporterName = b.getTransporter().getFullName();
        }
        if (b.getShop() != null) {
            r.shopId = b.getShop().getId();
            r.shopName = b.getShop().getFullName();
        }
        if (b.getCreatedBy() != null) r.createdById = b.getCreatedBy().getId();
        // Legacy
        if (b.getItem() != null) {
            r.itemId = b.getItem().getId();
            r.itemName = b.getItem().getName();
            r.itemImageUrl = b.getItem().getImageUrl();
        }
        // Multi-item combo
        if (b.getBatchItems() != null && !b.getBatchItems().isEmpty()) {
            r.batchItems = b.getBatchItems().stream()
                    .map(bi -> new BatchItemDto(
                            bi.getItem().getId(),
                            bi.getItem().getName(),
                            bi.getItem().getImageUrl(),
                            bi.getItem().getPriceTokens(),
                            bi.getQuantity()))
                    .collect(Collectors.toList());
        }
        r.createdAt = b.getCreatedAt();
        r.updatedAt = b.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getProvince() { return province; }
    public Integer getTotalPackages() { return totalPackages; }
    public Long getTokenPerPackage() { return tokenPerPackage; }
    public Integer getDeliveredCount() { return deliveredCount; }
    public ReliefBatchStatus getStatus() { return status; }
    public Long getTransporterId() { return transporterId; }
    public String getTransporterName() { return transporterName; }
    public Long getShopId() { return shopId; }
    public String getShopName() { return shopName; }
    public Long getCreatedById() { return createdById; }
    public Long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getItemImageUrl() { return itemImageUrl; }
    public List<BatchItemDto> getBatchItems() { return batchItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
