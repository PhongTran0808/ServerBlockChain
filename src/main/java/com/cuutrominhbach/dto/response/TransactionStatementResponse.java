package com.cuutrominhbach.dto.response;

import java.time.LocalDateTime;

public class TransactionStatementResponse {
    private Long id;
    private String type;
    private String eventType;
    private Long amount;
    private String note;
    private String txHash;
    private String fromName;
    private String toName;
    private Boolean isPlus;
    private LocalDateTime createdAt;

    public TransactionStatementResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }
    public Boolean getIsPlus() { return isPlus; }
    public void setIsPlus(Boolean isPlus) { this.isPlus = isPlus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
