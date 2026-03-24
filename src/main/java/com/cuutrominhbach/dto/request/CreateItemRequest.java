package com.cuutrominhbach.dto.request;

public class CreateItemRequest {
    private Long tokenId;
    private String name;
    private String imageUrl;
    private Long priceTokens;

    public CreateItemRequest() {}

    public Long getTokenId() { return tokenId; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public Long getPriceTokens() { return priceTokens; }

    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }
    public void setName(String name) { this.name = name; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setPriceTokens(Long priceTokens) { this.priceTokens = priceTokens; }
}
