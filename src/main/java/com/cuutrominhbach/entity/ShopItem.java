package com.cuutrominhbach.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_items")
public class ShopItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private User shop;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** Giá bán của shop — phải <= item.priceTokens (giá trần) */
    @Column(name = "shop_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal shopPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShopItemStatus status = ShopItemStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ShopItem() {}

    // Getters
    public Long getId() { return id; }
    public User getShop() { return shop; }
    public Item getItem() { return item; }
    public BigDecimal getShopPrice() { return shopPrice; }
    public Integer getQuantity() { return quantity; }
    public ShopItemStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setShop(User shop) { this.shop = shop; }
    public void setItem(Item item) { this.item = item; }
    public void setShopPrice(BigDecimal shopPrice) { this.shopPrice = shopPrice; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setStatus(ShopItemStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
