package com.cuutrominhbach.service;

import com.cuutrominhbach.dto.request.ShopItemRequest;
import com.cuutrominhbach.dto.response.ShopItemResponse;
import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.repository.ItemRepository;
import com.cuutrominhbach.repository.ShopItemRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopItemService {

    private final ShopItemRepository shopItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ShopItemService(ShopItemRepository shopItemRepository,
                           ItemRepository itemRepository,
                           UserRepository userRepository) {
        this.shopItemRepository = shopItemRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    /** Lấy toàn bộ kho hàng của shop */
    public List<ShopItemResponse> getInventory(Long shopId) {
        return shopItemRepository.findByShopId(shopId)
                .stream().map(ShopItemResponse::from).collect(Collectors.toList());
    }

    /** Thêm vật phẩm vào kho — validate giá trần */
    @Transactional
    public ShopItemResponse addToInventory(Long shopId, ShopItemRequest req) {
        User shop = userRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng"));
        if (shop.getRole() != Role.SHOP) {
            throw new IllegalArgumentException("Chỉ SHOP mới được quản lý kho hàng");
        }

        Item item = itemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vật phẩm #" + req.getItemId()));
        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new IllegalArgumentException("Vật phẩm này không còn trong danh mục chuẩn");
        }

        // Validate giá trần
        BigDecimal ceilingPrice = BigDecimal.valueOf(item.getPriceTokens());
        BigDecimal shopPrice = req.getShopPrice();
        if (shopPrice == null || shopPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0");
        }
        if (shopPrice.compareTo(ceilingPrice) > 0) {
            throw new IllegalArgumentException(
                    "Giá bán (" + shopPrice + " token) vượt quá giá trần (" + ceilingPrice + " token) của Admin");
        }

        // Không cho thêm trùng item (update thay vì thêm mới)
        if (shopItemRepository.existsByShopIdAndItemId(shopId, item.getId())) {
            throw new IllegalArgumentException(
                    "Vật phẩm này đã có trong kho. Hãy dùng chức năng Cập nhật.");
        }

        int qty = req.getQuantity() != null ? req.getQuantity() : 0;

        ShopItem si = new ShopItem();
        si.setShop(shop);
        si.setItem(item);
        si.setShopPrice(shopPrice);
        si.setQuantity(qty);
        si.setStatus(ShopItemStatus.ACTIVE);
        si.setCreatedAt(LocalDateTime.now());
        si.setUpdatedAt(LocalDateTime.now());

        return ShopItemResponse.from(shopItemRepository.save(si));
    }

    /** Cập nhật giá bán, số lượng, hoặc trạng thái */
    @Transactional
    public ShopItemResponse updateInventory(Long shopId, Long shopItemId, ShopItemRequest req) {
        ShopItem si = shopItemRepository.findById(shopItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục kho #" + shopItemId));

        // Đảm bảo shop chỉ sửa hàng của mình
        if (!si.getShop().getId().equals(shopId)) {
            throw new IllegalArgumentException("Bạn không có quyền sửa mục kho này");
        }

        // Validate giá trần nếu có cập nhật giá
        if (req.getShopPrice() != null) {
            BigDecimal ceilingPrice = BigDecimal.valueOf(si.getItem().getPriceTokens());
            if (req.getShopPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá bán phải lớn hơn 0");
            }
            if (req.getShopPrice().compareTo(ceilingPrice) > 0) {
                throw new IllegalArgumentException(
                        "Giá bán (" + req.getShopPrice() + ") vượt quá giá trần (" + ceilingPrice + ")");
            }
            si.setShopPrice(req.getShopPrice());
        }

        if (req.getQuantity() != null) {
            if (req.getQuantity() < 0) throw new IllegalArgumentException("Số lượng không được âm");
            si.setQuantity(req.getQuantity());
        }

        if (req.getStatus() != null) {
            try {
                si.setStatus(ShopItemStatus.valueOf(req.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ: " + req.getStatus());
            }
        }

        si.setUpdatedAt(LocalDateTime.now());
        return ShopItemResponse.from(shopItemRepository.save(si));
    }

    /** Giảm số lượng khi có đơn hàng — gọi từ EscrowService */
    @Transactional
    public void decrementQuantity(Long shopId, Long itemId) {
        shopItemRepository.findByShopIdAndStatus(shopId, ShopItemStatus.ACTIVE)
                .stream()
                .filter(si -> si.getItem().getId().equals(itemId))
                .findFirst()
                .ifPresent(si -> {
                    int newQty = Math.max(0, si.getQuantity() - 1);
                    si.setQuantity(newQty);
                    if (newQty == 0) si.setStatus(ShopItemStatus.INACTIVE);
                    si.setUpdatedAt(LocalDateTime.now());
                    shopItemRepository.save(si);
                });
    }
}
