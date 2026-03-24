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

    public enum TxType { IN, OUT }

    public TransactionHistory() {}

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

    public Long getId() { return id; }
    public Long getFromUserId() { return fromUserId; }
    public Long getToUserId() { return toUserId; }
    public TxType getType() { return type; }
    public Long getAmount() { return amount; }
    public String getNote() { return note; }
    public String getTxHash() { return txHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
    public void setType(TxType type) { this.type = type; }
    public void setAmount(Long amount) { this.amount = amount; }
    public void setNote(String note) { this.note = note; }
    public void setTxHash(String txHash) { this.txHash = txHash; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
