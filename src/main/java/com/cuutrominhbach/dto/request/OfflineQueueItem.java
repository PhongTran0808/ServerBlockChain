package com.cuutrominhbach.dto.request;

public class OfflineQueueItem {
    private Long orderId;
    private String citizenWalletAddress;
    private String citizenPin;
    private String scannedAt;

    public OfflineQueueItem() {}

    public Long getOrderId() { return orderId; }
    public String getCitizenWalletAddress() { return citizenWalletAddress; }
    public String getCitizenPin() { return citizenPin; }
    public String getScannedAt() { return scannedAt; }

    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public void setCitizenWalletAddress(String citizenWalletAddress) { this.citizenWalletAddress = citizenWalletAddress; }
    public void setCitizenPin(String citizenPin) { this.citizenPin = citizenPin; }
    public void setScannedAt(String scannedAt) { this.scannedAt = scannedAt; }
}
