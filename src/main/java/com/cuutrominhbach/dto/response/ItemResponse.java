package com.cuutrominhbach.dto.response;

import com.cuutrominhbach.entity.Item;
import com.cuutrominhbach.entity.ItemStatus;

public class ItemResponse {
    private Long id;
    private Long tokenId;
    private String name;
    private String imageUrl;
    private ItemStatus status;
    private Long priceTokens;

    public static ItemResponse from(Item item) {
        ItemResponse r = new ItemResponse();
        r.id = item.getId();
        r.tokenId = item.getTokenId();
        r.name = item.getName();
        r.imageUrl = item.getImageUrl();
        r.status = item.getStatus();
        r.priceTokens = item.getPriceTokens();
        return r;
    }

    public Long getId() { return id; }
    public Long getTokenId() { return tokenId; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public ItemStatus getStatus() { return status; }
    public Long getPriceTokens() { return priceTokens; }
}
