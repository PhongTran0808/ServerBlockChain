package com.cuutrominhbach.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "distribution_rounds")
public class DistributionRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_pool_id")
    private Long campaignPoolId;

    private String province;

    @Column(name = "campaign_code")
    private String campaignCode;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "recipients_count")
    private Integer recipientsCount;

    @Column(name = "share_amount")
    private Long shareAmount;

    @Column(name = "merkle_root")
    private String merkleRoot;

    @Column(name = "merkle_tx_hash")
    private String merkleTxHash;

    @Enumerated(EnumType.STRING)
    private DistributionRoundStatus status;

    @Lob
    @Column(name = "allocations_json", columnDefinition = "LONGTEXT")
    private String allocationsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public DistributionRound() {
    }

    public Long getId() { return id; }
    public Long getCampaignPoolId() { return campaignPoolId; }
    public String getProvince() { return province; }
    public String getCampaignCode() { return campaignCode; }
    public Long getTotalAmount() { return totalAmount; }
    public Integer getRecipientsCount() { return recipientsCount; }
    public Long getShareAmount() { return shareAmount; }
    public String getMerkleRoot() { return merkleRoot; }
    public String getMerkleTxHash() { return merkleTxHash; }
    public DistributionRoundStatus getStatus() { return status; }
    public String getAllocationsJson() { return allocationsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setCampaignPoolId(Long campaignPoolId) { this.campaignPoolId = campaignPoolId; }
    public void setProvince(String province) { this.province = province; }
    public void setCampaignCode(String campaignCode) { this.campaignCode = campaignCode; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public void setRecipientsCount(Integer recipientsCount) { this.recipientsCount = recipientsCount; }
    public void setShareAmount(Long shareAmount) { this.shareAmount = shareAmount; }
    public void setMerkleRoot(String merkleRoot) { this.merkleRoot = merkleRoot; }
    public void setMerkleTxHash(String merkleTxHash) { this.merkleTxHash = merkleTxHash; }
    public void setStatus(DistributionRoundStatus status) { this.status = status; }
    public void setAllocationsJson(String allocationsJson) { this.allocationsJson = allocationsJson; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
