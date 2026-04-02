package com.cuutrominhbach.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "to_user_id")
    private Long toUserId;

    @Enumerated(EnumType.STRING)
    private TxType type;

    private Long amount;
    private String note;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Truy vết lô cứu trợ — nullable, chỉ có khi giao dịch thuộc 1 lô */
    @Column(name = "batch_id")
    private Long batchId;

    public enum TxType {
        // ── Backward compat (data cũ) ──
        IN,        // Nhận tiền vào ví (generic)
        OUT,       // Xuất tiền khỏi ví (generic)
        TRANSFER,  // Chuyển khoản giữa 2 ví (generic)

        // ── Giai đoạn 1: Quyên góp ──
        DONATE,             // Nhà hảo tâm → Province Pool

        // ── Giai đoạn 2: Cứu trợ khẩn cấp ──
        ALLOCATE_ESCROW,    // Province Pool → Batch (tạm giữ khi Admin tạo lô)
        RECEIVE_RELIEF,     // Province Pool → Citizen (Dân được nhận cứu trợ, log 1 của giao hàng)
        PAY_SHOP,           // Citizen → Shop (Dân thanh toán ngay, log 2 của giao hàng)
        RETURN_SURPLUS,     // Citizen → Province Pool (Hoàn phần dư khi shopPrice < tokenPerPackage)

        // ── Giai đoạn 3: Phục hồi ──
        AIRDROP,            // Province Pool → Citizen (Admin chia thẳng tiền còn dư)
        WITHDRAW,           // Citizen → System (Dân rút tiền mặt)
    }


    public TransactionHistory() {}

    /** Constructor cũ — backward compatible */
    public TransactionHistory(Long fromUserId, Long toUserId, TxType type,
                               Long amount, String note, String txHash) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.txHash = txHash;
        this.createdAt = LocalDateTime.now();
    }

    /** Constructor mới — có batchId */
    public TransactionHistory(Long fromUserId, Long toUserId, TxType type,
                               Long amount, String note, String txHash, Long batchId) {
        this(fromUserId, toUserId, type, amount, note, txHash);
        this.batchId = batchId;
    }

    public Long getId() { return id; }
    public Long getFromUserId() { return fromUserId; }
    public Long getToUserId() { return toUserId; }
    public TxType getType() { return type; }
    public Long getAmount() { return amount; }
    public String getNote() { return note; }
    public String getTxHash() { return txHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getBatchId() { return batchId; }

    public void setId(Long id) { this.id = id; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
    public void setType(TxType type) { this.type = type; }
    public void setAmount(Long amount) { this.amount = amount; }
    public void setNote(String note) { this.note = note; }
    public void setTxHash(String txHash) { this.txHash = txHash; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
}
