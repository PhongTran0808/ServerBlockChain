package com.cuutrominhbach.service;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.dto.request.ConfirmDeliveryRequest;
import com.cuutrominhbach.dto.request.CreateOrderRequest;
import com.cuutrominhbach.dto.request.OfflineQueueItem;
import com.cuutrominhbach.dto.response.OrderResponse;
import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.repository.CampaignPoolRepository;
import com.cuutrominhbach.repository.ItemRepository;
import com.cuutrominhbach.repository.OrderRepository;
import com.cuutrominhbach.repository.TransactionHistoryRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EscrowService {

    private static final Logger log = LoggerFactory.getLogger(EscrowService.class);
    private static final BigInteger TOKEN_ID = BigInteger.ONE;

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CampaignPoolRepository campaignPoolRepository;
    private final TransactionHistoryRepository txHistoryRepository;
    private final BlockchainService blockchainService;
    private final PasswordEncoder passwordEncoder;

    public EscrowService(OrderRepository orderRepository,
                         ItemRepository itemRepository,
                         UserRepository userRepository,
                         CampaignPoolRepository campaignPoolRepository,
                         TransactionHistoryRepository txHistoryRepository,
                         BlockchainService blockchainService,
                         PasswordEncoder passwordEncoder) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.campaignPoolRepository = campaignPoolRepository;
        this.txHistoryRepository = txHistoryRepository;
        this.blockchainService = blockchainService;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NGHIỆP VỤ 1: Tạo đơn hàng với Price Ceiling & Spread Recovery
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public OrderResponse createOrder(Long citizenId, CreateOrderRequest req) {
        // 1. Validate citizen
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (citizen.getRole() != Role.CITIZEN) {
            throw new IllegalArgumentException("Chỉ CITIZEN mới có thể tạo đơn hàng");
        }
        if (!passwordEncoder.matches(req.getPin(), citizen.getHashPassword())) {
            throw new AuthException("PIN không đúng");
        }

        // 2. Validate item
        Item item = itemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vật phẩm"));
        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new IllegalArgumentException("Vật phẩm không còn khả dụng");
        }

        // 3. Validate shop
        User shop = userRepository.findById(req.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng"));

        // 4. Tính giá trần và shop_price
        long ceilingPrice = item.getPriceTokens();           // Giá trần — citizen luôn trả mức này
        long shopPrice = (req.getShopPrice() != null)
                ? req.getShopPrice()
                : ceilingPrice;                              // Nếu không truyền → không có chênh lệch

        if (shopPrice > ceilingPrice) {
            throw new IllegalArgumentException(
                    "Giá shop (" + shopPrice + ") không được vượt quá giá trần (" + ceilingPrice + ")");
        }
        if (shopPrice < 0) {
            throw new IllegalArgumentException("Giá shop không được âm");
        }

        long spreadAmount = ceilingPrice - shopPrice;        // Chênh lệch hoàn về quỹ tỉnh

        // 5. Lock toàn bộ ceiling price trên blockchain (escrow)
        String lockTxHash = blockchainService.lockTokens(
                citizen.getWalletAddress(),
                BigInteger.ZERO,                             // placeholder, cập nhật sau khi save
                BigInteger.valueOf(ceilingPrice)
        );

        // 6. Lưu đơn hàng
        Order order = Order.builder()
                .citizen(citizen)
                .shop(shop)
                .status(OrderStatus.PENDING)
                .totalTokens(ceilingPrice)
                .shopPrice(shopPrice)
                .refundAmount(spreadAmount)
                .lockTxHash(lockTxHash)
                .itemId(item.getId())
                .isFlagged(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        // 7. Nếu có chênh lệch → hoàn ngay về quỹ tỉnh của citizen
        if (spreadAmount > 0) {
            String spreadTxHash = refundSpreadToProvincePool(citizen, order.getId(), spreadAmount);
            order.setSpreadRefundTxHash(spreadTxHash);
            order = orderRepository.save(order);

            // Ghi lịch sử giao dịch
            txHistoryRepository.save(new TransactionHistory(
                    citizenId, null,
                    TransactionHistory.TxType.OUT,
                    spreadAmount,
                    "Hoàn chênh lệch giá trần về quỹ tỉnh (đơn #" + order.getId() + ")",
                    spreadTxHash
            ));
        }

        // Ghi lịch sử lock escrow
        txHistoryRepository.save(new TransactionHistory(
                citizenId, shop.getId(),
                TransactionHistory.TxType.OUT,
                shopPrice,
                "Đặt hàng #" + order.getId() + " - escrow cho shop",
                lockTxHash
        ));

        return OrderResponse.from(order);
    }

    /**
     * Hoàn chênh lệch (spread) về quỹ tỉnh của citizen.
     * Dùng transferToken từ ví citizen → ví quỹ tỉnh.
     */
    private String refundSpreadToProvincePool(User citizen, Long orderId, long spreadAmount) {
        String province = citizen.getProvince();
        if (province == null || province.isBlank()) {
            log.warn("Citizen {} không có tỉnh, bỏ qua hoàn chênh lệch", citizen.getId());
            return null;
        }

        CampaignPool pool = campaignPoolRepository.findByProvince(province)
                .orElse(null);
        if (pool == null) {
            log.warn("Không tìm thấy quỹ tỉnh '{}', bỏ qua hoàn chênh lệch", province);
            return null;
        }

        // Cập nhật số dư quỹ tỉnh trong DB
        pool.setTotalFund(pool.getTotalFund() + spreadAmount);
        pool.setUpdatedAt(LocalDateTime.now());
        campaignPoolRepository.save(pool);

        // Ghi nhận on-chain (best-effort)
        try {
            return blockchainService.mintToken(
                    citizen.getWalletAddress(),   // nguồn: ví citizen (đã bị lock, đây là ghi nhận)
                    TOKEN_ID,
                    BigInteger.valueOf(spreadAmount)
            );
        } catch (Exception e) {
            log.warn("Không thể ghi nhận spread refund on-chain cho đơn #{}: {}", orderId, e.getMessage());
            return "off-chain-spread-" + orderId;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NGHIỆP VỤ 2: Xử lý TNV làm mất hàng — resolveLostOrder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Admin gọi khi TNV làm mất hàng. Thực hiện 3 hành động trong 1 transaction:
     * 1. Hoàn 100% token (ceiling price) về ví citizen → status = REFUNDED_LOST
     * 2. Bồi thường shop_price từ quỹ tỉnh → chuyển token cho shop
     * 3. Phạt TNV: trừ token hoặc đánh dấu flagged
     */
    @Transactional
    public OrderResponse resolveLostOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));

        if (order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new IllegalArgumentException(
                    "Chỉ xử lý đơn đang IN_TRANSIT. Trạng thái hiện tại: " + order.getStatus());
        }

        User citizen = order.getCitizen();
        User shop = order.getShop();
        User transporter = order.getTransporter();
        long ceilingPrice = order.getTotalTokens();
        long shopPrice = order.getShopPrice() != null ? order.getShopPrice() : ceilingPrice;

        // ── BƯỚC 1: Hoàn tiền cho citizen ──────────────────────────────────
        String refundTxHash = blockchainService.releaseTokens(BigInteger.valueOf(orderId));
        txHistoryRepository.save(new TransactionHistory(
                null, citizen.getId(),
                TransactionHistory.TxType.IN,
                ceilingPrice,
                "Hoàn tiền đơn #" + orderId + " — TNV làm mất hàng",
                refundTxHash
        ));

        // ── BƯỚC 2: Bồi thường shop từ quỹ tỉnh ───────────────────────────
        String compensateTxHash = compensateShopFromPool(citizen, shop, orderId, shopPrice);

        // ── BƯỚC 3: Phạt TNV ───────────────────────────────────────────────
        String penaltyTxHash = penalizeTransporter(transporter, orderId, shopPrice);

        // ── Cập nhật trạng thái đơn ────────────────────────────────────────
        order.setStatus(OrderStatus.REFUNDED_LOST);
        order.setReleaseTxHash(refundTxHash);
        order.setIsFlagged(true);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        log.info("resolveLostOrder #{}: citizen refund={}, shop compensate={}, transporter penalty={}",
                orderId, refundTxHash, compensateTxHash, penaltyTxHash);

        return OrderResponse.from(order);
    }

    /**
     * Trích quỹ tỉnh bồi thường cho shop.
     */
    private String compensateShopFromPool(User citizen, User shop, Long orderId, long shopPrice) {
        if (shop == null) {
            log.warn("Đơn #{} không có shop, bỏ qua bồi thường", orderId);
            return null;
        }

        String province = citizen != null ? citizen.getProvince() : null;
        if (province != null) {
            campaignPoolRepository.findByProvince(province).ifPresent(pool -> {
                long deduct = Math.min(shopPrice, pool.getTotalFund());
                pool.setTotalFund(pool.getTotalFund() - deduct);
                pool.setUpdatedAt(LocalDateTime.now());
                campaignPoolRepository.save(pool);
            });
        }

        try {
            String txHash = blockchainService.mintToken(
                    shop.getWalletAddress(),
                    TOKEN_ID,
                    BigInteger.valueOf(shopPrice)
            );
            txHistoryRepository.save(new TransactionHistory(
                    null, shop.getId(),
                    TransactionHistory.TxType.IN,
                    shopPrice,
                    "Bồi thường đơn #" + orderId + " — TNV làm mất hàng",
                    txHash
            ));
            return txHash;
        } catch (Exception e) {
            log.error("Bồi thường shop thất bại cho đơn #{}: {}", orderId, e.getMessage());
            return null;
        }
    }

    /**
     * Phạt TNV: trừ token nếu có ví, luôn đánh dấu flagged trên đơn.
     */
    private String penalizeTransporter(User transporter, Long orderId, long penaltyAmount) {
        if (transporter == null) {
            log.warn("Đơn #{} không có transporter, bỏ qua phạt", orderId);
            return null;
        }

        // Đánh dấu vi phạm trên user (nếu muốn persist cần thêm field vào User)
        log.warn("TNV {} bị đánh dấu vi phạm do làm mất đơn #{}", transporter.getId(), orderId);

        try {
            // Trừ token từ ví TNV (best-effort — nếu không đủ thì chỉ ghi nhận)
            String txHash = blockchainService.transferToken(
                    transporter.getWalletAddress(),
                    "0x000000000000000000000000000000000000dEaD", // burn address
                    TOKEN_ID,
                    BigInteger.valueOf(penaltyAmount)
            );
            txHistoryRepository.save(new TransactionHistory(
                    transporter.getId(), null,
                    TransactionHistory.TxType.OUT,
                    penaltyAmount,
                    "Phạt vi phạm đơn #" + orderId + " — làm mất hàng",
                    txHash
            ));
            return txHash;
        } catch (Exception e) {
            // Không đủ token để phạt → chỉ ghi nhận, không throw
            log.warn("Không thể trừ token phạt TNV {} (đơn #{}): {}", transporter.getId(), orderId, e.getMessage());
            txHistoryRepository.save(new TransactionHistory(
                    transporter.getId(), null,
                    TransactionHistory.TxType.OUT,
                    0L,
                    "Vi phạm đơn #" + orderId + " — không đủ token để phạt, đã ghi nhận",
                    null
            ));
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Các method hiện có — giữ nguyên
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TNV quét QR citizen → xác nhận giao hàng thành công.
     * Mở khóa escrow (shopPrice) và chuyển thẳng vào ví shop trên blockchain.
     */
    @Transactional
    public OrderResponse confirmDelivery(Long orderId, ConfirmDeliveryRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new IllegalArgumentException("Đơn hàng phải ở trạng thái IN_TRANSIT để xác nhận giao");
        }

        User citizen = order.getCitizen();
        if (citizen == null) throw new IllegalArgumentException("Đơn hàng không có thông tin người nhận");

        // Xác thực: QR data phải khớp wallet address của citizen
        if (req.getQrData() != null && !req.getQrData().isBlank()
                && !req.getQrData().equalsIgnoreCase(citizen.getWalletAddress())) {
            throw new AuthException("Mã QR không khớp với người nhận đơn hàng này");
        }

        // Xác thực PIN citizen
        if (!passwordEncoder.matches(req.getCitizenPin(), citizen.getHashPassword())) {
            throw new AuthException("PIN không đúng");
        }

        User shop = order.getShop();
        long shopPrice = order.getShopPrice() != null ? order.getShopPrice() : order.getTotalTokens();

        // Mở khóa escrow trên blockchain
        String releaseTxHash = blockchainService.releaseTokens(BigInteger.valueOf(orderId));

        // Chuyển shopPrice vào ví shop
        String shopPayTxHash = null;
        if (shop != null && shop.getWalletAddress() != null) {
            try {
                shopPayTxHash = blockchainService.mintToken(
                        shop.getWalletAddress(),
                        TOKEN_ID,
                        BigInteger.valueOf(shopPrice)
                );
                txHistoryRepository.save(new TransactionHistory(
                        citizen.getId(), shop.getId(),
                        TransactionHistory.TxType.IN,
                        shopPrice,
                        "Thanh toán đơn #" + orderId + " — giao hàng thành công",
                        shopPayTxHash
                ));
            } catch (Exception e) {
                log.error("Không thể chuyển token cho shop sau giao hàng đơn #{}: {}", orderId, e.getMessage());
            }
        }

        // Ghi lịch sử citizen
        txHistoryRepository.save(new TransactionHistory(
                citizen.getId(), shop != null ? shop.getId() : null,
                TransactionHistory.TxType.OUT,
                shopPrice,
                "Thanh toán đơn #" + orderId + " — đã nhận hàng",
                releaseTxHash
        ));

        order.setStatus(OrderStatus.DELIVERED);
        order.setReleaseTxHash(releaseTxHash);
        order.setUpdatedAt(LocalDateTime.now());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public List<OrderResponse> syncOfflineQueue(Long transporterId, List<OfflineQueueItem> items) {
        User transporter = userRepository.findById(transporterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shipper"));
        if (transporter.getRole() != Role.TRANSPORTER) {
            throw new IllegalArgumentException("Chỉ TRANSPORTER mới được đồng bộ offline queue");
        }
        if (!Boolean.TRUE.equals(transporter.getIsApproved())) {
            throw new IllegalArgumentException("Shipper chưa được admin phê duyệt");
        }

        return items.stream().map(item -> {
            try {
                ConfirmDeliveryRequest req = new ConfirmDeliveryRequest();
                req.setCitizenPin(item.getCitizenPin());
                req.setQrData(item.getCitizenWalletAddress());
                return confirmDelivery(item.getOrderId(), req);
            } catch (Exception e) {
                log.warn("syncOfflineQueue: bỏ qua đơn {} — {}", item.getOrderId(), e.getMessage());
                return null;
            }
        }).filter(r -> r != null).collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByCitizen(Long citizenId) {
        return orderRepository.findByCitizenId(citizenId)
                .stream().map(OrderResponse::from).collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByShop(Long shopId) {
        return orderRepository.findByShopId(shopId)
                .stream().map(OrderResponse::from).collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByShopAndStatus(Long shopId, String statusStr) {
        try {
            OrderStatus status = OrderStatus.valueOf(statusStr.toUpperCase());
            return orderRepository.findByShopIdAndStatus(shopId, status)
                    .stream().map(OrderResponse::from).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + statusStr);
        }
    }

    public List<OrderResponse> getOrdersByTransporter(Long transporterId) {
        return orderRepository.findByTransporterId(transporterId)
                .stream().map(OrderResponse::from).collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream().map(OrderResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse markReady(Long orderId, Long shopId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        User shop = userRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng"));

        if (shop.getRole() != Role.SHOP) throw new IllegalArgumentException("Chỉ SHOP mới được đánh dấu đơn READY");
        if (!Boolean.TRUE.equals(shop.getIsApproved())) throw new IllegalArgumentException("Cửa hàng chưa được admin phê duyệt");
        if (order.getShop() == null || !order.getShop().getId().equals(shopId)) {
            throw new IllegalArgumentException("Bạn không có quyền thao tác đơn hàng này");
        }

        order.setStatus(OrderStatus.READY);
        order.setUpdatedAt(LocalDateTime.now());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse markPickup(Long orderId, Long transporterId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        User transporter = userRepository.findById(transporterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shipper"));

        if (!Boolean.TRUE.equals(transporter.getIsApproved())) throw new IllegalArgumentException("Shipper chưa được admin phê duyệt");
        if (transporter.getRole() != Role.TRANSPORTER) throw new IllegalArgumentException("Chỉ TRANSPORTER mới được nhận đơn");

        order.setTransporter(transporter);
        order.setStatus(OrderStatus.IN_TRANSIT);
        order.setUpdatedAt(LocalDateTime.now());
        return OrderResponse.from(orderRepository.save(order));
    }
}
