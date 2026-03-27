package com.cuutrominhbach.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class ClaimableDistributionResponse {
    private Long roundId;
    private String province;
    private String campaignCode;
    private Long amount;
    private String merkleRoot;
    private List<String> proof;
    private boolean claimed;
    private LocalDateTime createdAt;

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCampaignCode() { return campaignCode; }
    public void setCampaignCode(String campaignCode) { this.campaignCode = campaignCode; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getMerkleRoot() { return merkleRoot; }
    public void setMerkleRoot(String merkleRoot) { this.merkleRoot = merkleRoot; }
    public List<String> getProof() { return proof; }
    public void setProof(List<String> proof) { this.proof = proof; }
    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
