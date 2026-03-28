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
    private Long batchId;
    private LocalDateTime createdAt;

    public TransactionResponse() {}

    public static TransactionResponse from(TransactionHistory t) {
        TransactionResponse r = new TransactionResponse();
        r.id = t.getId();
        r.type = t.getType().name();
        r.eventType = resolveEventType(t.getType(), t.getNote(), t.getBatchId());
        r.amount = t.getAmount();
        r.note = t.getNote();
        r.txHash = t.getTxHash();
        r.fromUserId = t.getFromUserId();
        r.toUserId = t.getToUserId();
        r.batchId = t.getBatchId();
        r.createdAt = t.getCreatedAt();
        return r;
    }

    private static String resolveEventType(TransactionHistory.TxType type, String note, Long batchId) {
        if (type == null) return "UNKNOWN";

        // Giai đoạn 1: Quyên góp
        if (type == TransactionHistory.TxType.DONATE)           return "DONATE";
        // Giai đoạn 2: Cứu trợ khẩn cấp
        if (type == TransactionHistory.TxType.ALLOCATE_ESCROW)  return "ALLOCATE_ESCROW";
        if (type == TransactionHistory.TxType.RECEIVE_RELIEF)   return "RECEIVE_RELIEF";
        if (type == TransactionHistory.TxType.PAY_SHOP)         return "PAY_SHOP";
        // Giai đoạn 3: Phục hồi
        if (type == TransactionHistory.TxType.AIRDROP)          return "AIRDROP";
        if (type == TransactionHistory.TxType.WITHDRAW)         return "WITHDRAW";

        // Backward compat: data cũ dùng IN/OUT/TRANSFER — phân loại qua note
        if (note == null || note.isBlank()) return type.name();
        String n = note.trim().toLowerCase();
        if (n.startsWith("nạp tiền"))           return "TOPUP";
        if (n.startsWith("quyên góp"))          return "DONATE";
        if (n.startsWith("nhận cứu trợ campaign")) return "AIRDROP";
        if (n.startsWith("nhận cứu trợ claim"))   return "CLAIM";
        if (n.startsWith("rút tiền") || n.startsWith("quy đổi token")) return "WITHDRAW";
        if (n.startsWith("thanh toán đơn"))     return "ORDER_PAYMENT";
        if (batchId != null)                     return "RELIEF_BATCH";

        return type.name();
    }


    public Long getId() { return id; }
    public String getType() { return type; }
    public String getEventType() { return eventType; }
    public Long getAmount() { return amount; }
    public String getNote() { return note; }
    public String getTxHash() { return txHash; }
    public Long getFromUserId() { return fromUserId; }
    public Long getToUserId() { return toUserId; }
    public Long getBatchId() { return batchId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
