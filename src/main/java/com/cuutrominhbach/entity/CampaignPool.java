package com.cuutrominhbach.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_pools")
public class CampaignPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_code")
    private String campaignCode;

    private String province;

    @Column(name = "total_fund")
    private Long totalFund;

    @Column(name = "is_receiving_active")
    private Boolean isReceivingActive = true;

    @Column(name = "is_auto_airdrop")
    private Boolean isAutoAirdrop = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CampaignPool() {}

    public CampaignPool(Long id, String campaignCode, String province, Long totalFund, Boolean isReceivingActive, Boolean isAutoAirdrop, LocalDateTime updatedAt) {
        this.id = id;
        this.campaignCode = campaignCode;
        this.province = province;
        this.totalFund = totalFund;
        this.isReceivingActive = isReceivingActive;
        this.isAutoAirdrop = isAutoAirdrop;
        this.updatedAt = updatedAt;
    }

    // Getters
    public Long getId() { return id; }
    public String getCampaignCode() { return campaignCode; }
    public String getProvince() { return province; }
    public Long getTotalFund() { return totalFund; }
    public Boolean getIsReceivingActive() { return isReceivingActive; }
    public Boolean getIsAutoAirdrop() { return isAutoAirdrop; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setCampaignCode(String campaignCode) { this.campaignCode = campaignCode; }
    public void setProvince(String province) { this.province = province; }
    public void setTotalFund(Long totalFund) { this.totalFund = totalFund; }
    public void setIsReceivingActive(Boolean isReceivingActive) { this.isReceivingActive = isReceivingActive; }
    public void setIsAutoAirdrop(Boolean isAutoAirdrop) { this.isAutoAirdrop = isAutoAirdrop; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String campaignCode;
        private String province;
        private Long totalFund;
        private Boolean isReceivingActive = true;
        private Boolean isAutoAirdrop = false;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder campaignCode(String campaignCode) { this.campaignCode = campaignCode; return this; }
        public Builder province(String province) { this.province = province; return this; }
        public Builder totalFund(Long totalFund) { this.totalFund = totalFund; return this; }
        public Builder isReceivingActive(Boolean v) { this.isReceivingActive = v; return this; }
        public Builder isAutoAirdrop(Boolean v) { this.isAutoAirdrop = v; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public CampaignPool build() {
            return new CampaignPool(id, campaignCode, province, totalFund, isReceivingActive, isAutoAirdrop, updatedAt);
        }
    }
}
