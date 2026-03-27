package com.cuutrominhbach.dto.response;

import com.cuutrominhbach.entity.DistributionRound;

import java.time.LocalDateTime;

public class DistributionRoundResponse {
    private Long id;
    private String province;
    private String campaignCode;
    private Long totalAmount;
    private Integer recipientsCount;
    private Long shareAmount;
    private String merkleRoot;
    private String merkleTxHash;
    private String status;
    private LocalDateTime createdAt;

    public static DistributionRoundResponse from(DistributionRound r) {
        DistributionRoundResponse dto = new DistributionRoundResponse();
        dto.id = r.getId();
        dto.province = r.getProvince();
        dto.campaignCode = r.getCampaignCode();
        dto.totalAmount = r.getTotalAmount();
        dto.recipientsCount = r.getRecipientsCount();
        dto.shareAmount = r.getShareAmount();
        dto.merkleRoot = r.getMerkleRoot();
        dto.merkleTxHash = r.getMerkleTxHash();
        dto.status = r.getStatus() != null ? r.getStatus().name() : null;
        dto.createdAt = r.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getProvince() { return province; }
    public String getCampaignCode() { return campaignCode; }
    public Long getTotalAmount() { return totalAmount; }
    public Integer getRecipientsCount() { return recipientsCount; }
    public Long getShareAmount() { return shareAmount; }
    public String getMerkleRoot() { return merkleRoot; }
    public String getMerkleTxHash() { return merkleTxHash; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
