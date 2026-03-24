package com.cuutrominhbach.dto.response;

import com.cuutrominhbach.entity.TransactionHistory;
import java.time.LocalDateTime;

public class TransactionResponse {
    private Long id;
    private String type;
    private Long amount;
    private String note;
    private String txHash;
    private Long fromUserId;
    private Long toUserId;
    private LocalDateTime createdAt;

    public TransactionResponse() {}

    public static TransactionResponse from(TransactionHistory t) {
        TransactionResponse r = new TransactionResponse();
        r.id = t.getId();
        r.type = t.getType().name();
        r.amount = t.getAmount();
        r.note = t.getNote();
        r.txHash = t.getTxHash();
        r.fromUserId = t.getFromUserId();
        r.toUserId = t.getToUserId();
        r.createdAt = t.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public Long getAmount() { return amount; }
    public String getNote() { return note; }
    public String getTxHash() { return txHash; }
    public Long getFromUserId() { return fromUserId; }
    public Long getToUserId() { return toUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
