package com.cuutrominhbach.dto.request;

public class ConfirmDeliveryRequest {
    private String citizenPin;
    private String qrData;

    public ConfirmDeliveryRequest() {}

    public String getCitizenPin() { return citizenPin; }
    public String getQrData() { return qrData; }

    public void setCitizenPin(String citizenPin) { this.citizenPin = citizenPin; }
    public void setQrData(String qrData) { this.qrData = qrData; }
}
