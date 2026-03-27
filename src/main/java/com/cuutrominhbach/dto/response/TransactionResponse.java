package com.cuutrominhbach.dto.response;

import com.cuutrominhbach.entity.TransactionHistory;
import java.time.LocalDateTime;

public class TransactionResponse {
    private Long id;
    private String type;
    private String eventType;
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
        r.eventType = resolveEventType(t.getNote());
        r.amount = t.getAmount();
        r.note = t.getNote();
        r.txHash = t.getTxHash();
        r.fromUserId = t.getFromUserId();
        r.toUserId = t.getToUserId();
        r.createdAt = t.getCreatedAt();
        return r;
    }

    private static String resolveEventType(String note) {
        if (note == null || note.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = note.trim().toLowerCase();
        if (normalized.startsWith("nap tien")) return "TOPUP";
        if (normalized.startsWith("nạp tiền")) return "TOPUP";
        if (normalized.startsWith("quyen gop")) return "DONATE";
        if (normalized.startsWith("quyên góp")) return "DONATE";
        if (normalized.startsWith("nhan cuu tro campaign")) return "AIRDROP";
        if (normalized.startsWith("nhận cứu trợ campaign")) return "AIRDROP";
        if (normalized.startsWith("nhan cuu tro claim")) return "CLAIM";
        if (normalized.startsWith("nhận cứu trợ claim")) return "CLAIM";

        return "UNKNOWN";
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getEventType() { return eventType; }
    public Long getAmount() { return amount; }
    public String getNote() { return note; }
    public String getTxHash() { return txHash; }
    public Long getFromUserId() { return fromUserId; }
    public Long getToUserId() { return toUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
