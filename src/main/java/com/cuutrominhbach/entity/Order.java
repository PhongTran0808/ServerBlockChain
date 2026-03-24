package com.cuutrominhbach.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private User shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id")
    private User transporter;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "lock_tx_hash")
    private String lockTxHash;

    @Column(name = "release_tx_hash")
    private String releaseTxHash;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Order() {}

    // Getters
    public Long getId() { return id; }
    public User getCitizen() { return citizen; }
    public User getShop() { return shop; }
    public User getTransporter() { return transporter; }
    public OrderStatus getStatus() { return status; }
    public Long getTotalTokens() { return totalTokens; }
    public String getLockTxHash() { return lockTxHash; }
    public String getReleaseTxHash() { return releaseTxHash; }
    public Long getItemId() { return itemId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setCitizen(User citizen) { this.citizen = citizen; }
    public void setShop(User shop) { this.shop = shop; }
    public void setTransporter(User transporter) { this.transporter = transporter; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    public void setLockTxHash(String lockTxHash) { this.lockTxHash = lockTxHash; }
    public void setReleaseTxHash(String releaseTxHash) { this.releaseTxHash = releaseTxHash; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private User citizen;
        private User shop;
        private User transporter;
        private OrderStatus status;
        private Long totalTokens;
        private String lockTxHash;
        private String releaseTxHash;
        private Long itemId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder citizen(User citizen) { this.citizen = citizen; return this; }
        public Builder shop(User shop) { this.shop = shop; return this; }
        public Builder transporter(User transporter) { this.transporter = transporter; return this; }
        public Builder status(OrderStatus status) { this.status = status; return this; }
        public Builder totalTokens(Long totalTokens) { this.totalTokens = totalTokens; return this; }
        public Builder lockTxHash(String lockTxHash) { this.lockTxHash = lockTxHash; return this; }
        public Builder releaseTxHash(String releaseTxHash) { this.releaseTxHash = releaseTxHash; return this; }
        public Builder itemId(Long itemId) { this.itemId = itemId; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Order build() {
            Order o = new Order();
            o.id = id; o.citizen = citizen; o.shop = shop; o.transporter = transporter;
            o.status = status; o.totalTokens = totalTokens; o.lockTxHash = lockTxHash;
            o.releaseTxHash = releaseTxHash; o.itemId = itemId;
            o.createdAt = createdAt; o.updatedAt = updatedAt;
            return o;
        }
    }
}
