package com.cuutrominhbach.entity;

import jakarta.persistence.*;
import java.math.BigInteger;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id")
    private Long tokenId;

    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ItemStatus status;

    @Column(name = "price_tokens")
    private Long priceTokens;

    public Item() {}

    public Item(Long id, Long tokenId, String name, String imageUrl, ItemStatus status, Long priceTokens) {
        this.id = id;
        this.tokenId = tokenId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.status = status;
        this.priceTokens = priceTokens;
    }

    public Long getId() { return id; }
    public Long getTokenId() { return tokenId; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public ItemStatus getStatus() { return status; }
    public Long getPriceTokens() { return priceTokens; }

    public void setId(Long id) { this.id = id; }
    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }
    public void setName(String name) { this.name = name; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public void setPriceTokens(Long priceTokens) { this.priceTokens = priceTokens; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tokenId;
        private String name;
        private String imageUrl;
        private ItemStatus status;
        private Long priceTokens;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tokenId(Long tokenId) { this.tokenId = tokenId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder status(ItemStatus status) { this.status = status; return this; }
        public Builder priceTokens(Long priceTokens) { this.priceTokens = priceTokens; return this; }

        public Item build() {
            return new Item(id, tokenId, name, imageUrl, status, priceTokens);
        }
    }
}
