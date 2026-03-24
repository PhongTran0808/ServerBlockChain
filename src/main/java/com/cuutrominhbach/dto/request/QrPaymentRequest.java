package com.cuutrominhbach.dto.request;

public class QrPaymentRequest {
    private String citizenWalletAddress;
    private Long tokenId;
    private Long amount;

    public QrPaymentRequest() {}

    public String getCitizenWalletAddress() { return citizenWalletAddress; }
    public Long getTokenId() { return tokenId; }
    public Long getAmount() { return amount; }

    public void setCitizenWalletAddress(String citizenWalletAddress) { this.citizenWalletAddress = citizenWalletAddress; }
    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }
    public void setAmount(Long amount) { this.amount = amount; }
}
