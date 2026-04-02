package com.cuutrominhbach.service;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.dto.response.ReliefBatchResponse;
import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Isolation;
import jakarta.persistence.EntityManager;

@Service
public class ReliefBatchService {

    private static final Logger log = LoggerFactory.getLogger(ReliefBatchService.class);
    private static final BigInteger TOKEN_ID = BigInteger.ONE;

    private final ReliefBatchRepository batchRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BatchItemRepository batchItemRepository;
    private final ShopItemRepository shopItemRepository;
    private final CampaignPoolRepository campaignPoolRepository;
    private final TransactionHistoryRepository txRepository;
    private final BlockchainService blockchainService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public ReliefBatchService(ReliefBatchRepository batchRepository,
                               UserRepository userRepository,
                               ItemRepository itemRepository,
                               BatchItemRepository batchItemRepository,
                               ShopItemRepository shopItemRepository,
                               CampaignPoolRepository campaignPoolRepository,
                               TransactionHistoryRepository txRepository,
                               BlockchainService blockchainService,
                               PasswordEncoder passwordEncoder,
                               EntityManager entityManager) {
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.batchItemRepository = batchItemRepository;
        this.shopItemRepository = shopItemRepository;
        this.campaignPoolRepository = campaignPoolRepository;
        this.txRepository = txRepository;
        this.blockchainService = blockchainService;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    // ── ADMIN: Tạo lô cứu trợ ────────────────────────────────────────────────

    @Transactional
    @SuppressWarnings("unchecked")
    public ReliefBatchResponse createBatch(Long adminId, Map<String, Object> body) {
        String name = (String) body.get("name");
        String province = (String) body.get("province");

        if (name == null || name.isBlank()) throw new IllegalArgumentException("Tên lô không được trống");
        if (province == null || province.isBlank()) throw new IllegalArgumentException("Tỉnh không được trống");

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy admin"));

        // Auto totalPackages = số citizen của tỉnh
        int totalPackages;
        if (body.get("totalPackages") != null) {
            totalPackages = Integer.parseInt(body.get("totalPackages").toString());
        } else {
            totalPackages = (int) userRepository.countByRoleAndProvince(Role.CITIZEN, province);
        }
        if (totalPackages <= 0) throw new IllegalArgumentException("Tỉnh này chưa có người dân nào");

        // Xử lý multi-items: [{ itemId, quantity }]
        List<Map<String, Object>> itemsPayload = body.get("items") != null
                ? (List<Map<String, Object>>) body.get("items")
                : List.of();

        // Tính tokenPerPackage từ combo items
        long tokenPerPackage = 0;
        if (!itemsPayload.isEmpty()) {
            for (Map<String, Object> entry : itemsPayload) {
                Long itemId = Long.parseLong(entry.get("itemId").toString());
                int qty = entry.get("quantity") != null ? Integer.parseInt(entry.get("quantity").toString()) : 1;
                Item item = itemRepository.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vật phẩm #" + itemId));
                tokenPerPackage += item.getPriceTokens() * qty;
            }
        } else if (body.get("tokenPerPackage") != null) {
            tokenPerPackage = Long.parseLong(body.get("tokenPerPackage").toString());
        } else {
            throw new IllegalArgumentException("Cần chọn ít nhất 1 vật phẩm hoặc nhập Token/phần");
        }
        if (tokenPerPackage <= 0) throw new IllegalArgumentException("Token/phần phải > 0");

        // Legacy single item
        Item legacyItem = null;
        if (body.get("itemId") != null) {
            legacyItem = itemRepository.findById(Long.parseLong(body.get("itemId").toString())).orElse(null);
        }

        ReliefBatch batch = new ReliefBatch();
        batch.setName(name);
        batch.setProvince(province);
        batch.setTotalPackages(totalPackages);
        batch.setTokenPerPackage(tokenPerPackage);
        batch.setDeliveredCount(0);
        batch.setStatus(ReliefBatchStatus.CREATED);
        batch.setCreatedBy(admin);
        batch.setItem(legacyItem);
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());

        ReliefBatch saved = batchRepository.save(batch);

        // Lưu batch_items
        for (Map<String, Object> entry : itemsPayload) {
            Long itemId = Long.parseLong(entry.get("itemId").toString());
            int qty = entry.get("quantity") != null ? Integer.parseInt(entry.get("quantity").toString()) : 1;
            Item item = itemRepository.findById(itemId).orElseThrow();
            batchItemRepository.save(new BatchItem(saved, item, qty));
        }

        // Ghi log ALLOCATE_ESCROW: Province Pool → Batch (tạm giữ)
        long totalEscrow = tokenPerPackage * totalPackages;
        txRepository.save(new TransactionHistory(
                null, null, // from = System (Pool), to = System (Batch Escrow)
                TransactionHistory.TxType.ALLOCATE_ESCROW,
                totalEscrow,
                "Khoá quỹ tạo lô hàng: " + name + " (" + province + ")",
                null,
                saved.getId()
        ));

        // Reload để lấy batchItems
        return ReliefBatchResponse.from(batchRepository.findById(saved.getId()).orElse(saved));
    }

    // ── ADMIN: Thống kê tỉnh ─────────────────────────────────────────────────

    public Map<String, Object> getProvinceStats(String province) {
        long totalCitizens = userRepository.countByRoleAndProvince(Role.CITIZEN, province);
        long availableTokens = campaignPoolRepository.findByProvince(province)
                .map(p -> p.getTotalFund() != null ? p.getTotalFund() : 0L)
                .orElse(0L);
        return Map.of(
                "province", province,
                "totalCitizens", totalCitizens,
                "availableTokens", availableTokens
        );
    }

    // ── ADMIN: Xóa lô (chỉ khi CREATED) ─────────────────────────────────────

    @Transactional
    public void deleteBatch(Long batchId, Long adminId) {
        ReliefBatch batch = getBatchOrThrow(batchId);
        if (batch.getStatus() != ReliefBatchStatus.CREATED) {
            throw new IllegalArgumentException("Chỉ có thể xóa lô ở trạng thái CREATED");
        }
        batchRepository.deleteById(batchId);
    }

    // ── ADMIN: Lấy tất cả lô ─────────────────────────────────────────────────

    public List<ReliefBatchResponse> getAllBatches() {
        return batchRepository.findAllWithItems().stream()
                .map(ReliefBatchResponse::from).collect(Collectors.toList());
    }

    // ── Lấy chi tiết 1 lô theo ID (dùng chung) ────────────────────────────

    public ReliefBatchResponse getBatchById(Long id) {
        return ReliefBatchResponse.from(getBatchOrThrow(id));
    }

    // ── TNV: Xem lô theo tỉnh (status = CREATED) ─────────────────────────────

    public List<ReliefBatchResponse> getAvailableBatches(String province) {
        List<ReliefBatch> batches = (province != null && !province.isBlank())
                ? batchRepository.findByProvinceAndStatus(province, ReliefBatchStatus.CREATED)
                : batchRepository.findByStatus(ReliefBatchStatus.CREATED);
        return batches.stream().map(ReliefBatchResponse::from).collect(Collectors.toList());
    }

    // ── TNV: Xem lô của mình ─────────────────────────────────────────────────

    public List<ReliefBatchResponse> getMyBatches(Long transporterId) {
        return batchRepository.findByTransporterId(transporterId).stream()
                .map(ReliefBatchResponse::from).collect(Collectors.toList());
    }

    // ── TNV: Nhận lô + chọn shop ─────────────────────────────────────────────

    @Transactional
    public ReliefBatchResponse claimBatch(Long batchId, Long transporterId, Long shopId) {
        // Sử dụng Lock bi quan để tránh 2 TNV cùng nhận 1 lô hàng
        ReliefBatch batch = batchRepository.findWithLockById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô cứu trợ #" + batchId));

        if (batch.getStatus() != ReliefBatchStatus.CREATED
                && batch.getStatus() != ReliefBatchStatus.SHOP_REJECTED) {
            throw new IllegalArgumentException("Lô này không thể nhận (trạng thái: " + batch.getStatus() + ")");
        }

        User transporter = userRepository.findById(transporterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy TNV"));
        if (transporter.getRole() != Role.TRANSPORTER) {
            throw new IllegalArgumentException("Chỉ TRANSPORTER mới được nhận lô");
        }

        User shop = userRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Shop"));
        if (shop.getRole() != Role.SHOP) {
            throw new IllegalArgumentException("shopId phải là tài khoản SHOP");
        }

        if (shop.getWalletAddress() == null || shop.getWalletAddress().isBlank()) {
            throw new IllegalArgumentException("Cửa hàng này chưa được thiết lập địa chỉ ví Blockchain. Vui lòng liên hệ Admin.");
        }

        // Kiểm tra tồn kho: shop phải có đủ số lượng từng vật phẩm trong lô
        List<BatchItem> batchItems = batch.getBatchItems();
        if (!batchItems.isEmpty()) {
            List<com.cuutrominhbach.entity.ShopItem> shopInventory =
                    shopItemRepository.findByShopIdAndStatus(shopId, com.cuutrominhbach.entity.ShopItemStatus.ACTIVE);

            for (BatchItem bi : batchItems) {
                Long itemId = bi.getItem().getId();
                int requiredQty = bi.getQuantity() * batch.getTotalPackages(); // qty/phần × số phần
                int shopQty = shopInventory.stream()
                        .filter(si -> si.getItem().getId().equals(itemId))
                        .mapToInt(com.cuutrominhbach.entity.ShopItem::getQuantity)
                        .sum();
                if (shopQty < requiredQty) {
                    throw new IllegalArgumentException(
                            "Cửa hàng không đủ số lượng vật phẩm để cung cấp lô hàng này" +
                            " (cần " + requiredQty + " " + bi.getItem().getName() + ", shop có " + shopQty + ")");
                }
            }
        }

        batch.setTransporter(transporter);
        batch.setShop(shop);
        batch.setStatus(ReliefBatchStatus.WAITING_SHOP);
        batch.setUpdatedAt(LocalDateTime.now());
        return ReliefBatchResponse.from(batchRepository.save(batch));
    }

    // ── SHOP: Xem lô đang chờ duyệt ──────────────────────────────────────────

    public List<ReliefBatchResponse> getPendingBatchesForShop(Long shopId) {
        return batchRepository.findByShopIdAndStatus(shopId, ReliefBatchStatus.WAITING_SHOP)
                .stream().map(ReliefBatchResponse::from).collect(Collectors.toList());
    }

    public List<ReliefBatchResponse> getAllBatchesForShop(Long shopId) {
        return batchRepository.findByShopId(shopId).stream()
                .map(ReliefBatchResponse::from).collect(Collectors.toList());
    }

    // ── SHOP: Chấp nhận lô ───────────────────────────────────────────────────

    @Transactional
    public ReliefBatchResponse acceptBatch(Long batchId, Long shopId) {
        ReliefBatch batch = getBatchOrThrow(batchId);
        validateShopOwnership(batch, shopId);
        if (batch.getStatus() != ReliefBatchStatus.WAITING_SHOP) {
            throw new IllegalArgumentException("Lô không ở trạng thái WAITING_SHOP");
        }
        batch.setStatus(ReliefBatchStatus.ACCEPTED);
        batch.setUpdatedAt(LocalDateTime.now());
        return ReliefBatchResponse.from(batchRepository.save(batch));
    }

    // ── SHOP: Từ chối lô ─────────────────────────────────────────────────────

    @Transactional
    public ReliefBatchResponse rejectBatch(Long batchId, Long shopId) {
        ReliefBatch batch = getBatchOrThrow(batchId);
        validateShopOwnership(batch, shopId);
        if (batch.getStatus() != ReliefBatchStatus.WAITING_SHOP) {
            throw new IllegalArgumentException("Lô không ở trạng thái WAITING_SHOP");
        }
        // Giữ transporter, chỉ reset shop để TNV chọn shop khác
        batch.setShop(null);
        batch.setStatus(ReliefBatchStatus.SHOP_REJECTED);
        batch.setUpdatedAt(LocalDateTime.now());
        return ReliefBatchResponse.from(batchRepository.save(batch));
    }

    // ── TNV: Quét QR Shop → lấy hàng ─────────────────────────────────────────

    @Transactional
    public ReliefBatchResponse pickupBatch(Long batchId, Long transporterId, String qrData) {
        ReliefBatch batch = getBatchOrThrow(batchId);

        if (batch.getStatus() != ReliefBatchStatus.ACCEPTED) {
            throw new IllegalArgumentException("Lô chưa được Shop chấp nhận");
        }
        if (batch.getTransporter() == null) {
            throw new IllegalArgumentException("Lô này chưa được phân công TNV nào (transporter is null)");
        }
        if (!batch.getTransporter().getId().equals(transporterId)) {
            throw new IllegalArgumentException(String.format(
                "Bạn không phải TNV của lô này! (Lô thuộc về ID: %s, Bạn là ID: %s)", 
                batch.getTransporter().getId(), transporterId));
        }

        // Validate QR: phải chứa batchId
        String expectedQr = "BATCH:" + batchId;
        if (qrData == null || !qrData.equals(expectedQr)) {
            throw new IllegalArgumentException("Mã QR không hợp lệ cho lô #" + batchId);
        }

        batch.setStatus(ReliefBatchStatus.PICKED_UP);
        batch.setUpdatedAt(LocalDateTime.now());
        return ReliefBatchResponse.from(batchRepository.save(batch));
    }

    // ── TNV: Quét QR Citizen → phân phát 1 phần ──────────────────────────────

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReliefBatchResponse deliverToOneCitizen(Long batchId, Long transporterId, String citizenWalletQr) {
        // Sử dụng Lock bi quan để tránh race condition khi phân phát
        ReliefBatch batch = batchRepository.findWithLockById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô cứu trợ #" + batchId));

        if (batch.getStatus() != ReliefBatchStatus.PICKED_UP
                && batch.getStatus() != ReliefBatchStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Lô chưa được lấy hàng (cần PICKED_UP hoặc IN_PROGRESS)");
        }
        if (batch.getTransporter() == null) {
            throw new IllegalArgumentException("Lô này chưa được phân công TNV nào (transporter is null)");
        }
        if (!batch.getTransporter().getId().equals(transporterId)) {
            throw new IllegalArgumentException(String.format(
                "Bạn không phải TNV của lô này! (Lô thuộc về ID: %s, Bạn là ID: %s)", 
                batch.getTransporter().getId(), transporterId));
        }
        if (batch.getDeliveredCount() >= batch.getTotalPackages()) {
            throw new IllegalArgumentException("Lô đã phân phát hết");
        }

        // Tìm citizen theo wallet address từ QR (làm sạch dữ liệu QR trước)
        String cleanedWallet = citizenWalletQr != null ? citizenWalletQr.trim() : "";
        User citizen = userRepository.findFirstByWalletAddress(cleanedWallet)
                .filter(u -> u.getRole() == Role.CITIZEN)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dân với mã QR này (" + cleanedWallet + ")"));

        // ── VALIDATE TỈNH: Citizen phải thuộc đúng tỉnh của lô ──────────────
        String batchProvince = batch.getProvince();
        String citizenProvince = citizen.getProvince();
        if (citizenProvince == null || !citizenProvince.equalsIgnoreCase(batchProvince)) {
            throw new IllegalArgumentException(
                "Người dân này thuộc tỉnh '" + (citizenProvince != null ? citizenProvince : "không xác định")
                + "', không thuộc lô cứu trợ tỉnh '" + batchProvince + "'");
        }

        // Kiểm tra đã nhận chưa
        boolean alreadyReceived = txRepository.existsByBatchIdAndTypeAndToUserId(
                batchId, TransactionHistory.TxType.RECEIVE_RELIEF, citizen.getId());
        if (alreadyReceived) {
            throw new IllegalArgumentException("Người dân này đã nhận hàng từ lô #" + batchId + " rồi");
        }

        // Xóa cache của Hibernate để đảm bảo không lấy dữ liệu cũ/sai lệch
        entityManager.refresh(batch);

        // Lấy Shop bằng native query để bypass hoàn toàn Hibernate cache/proxy
        // — đây là fix cho lô cũ: shop có thể đã được cập nhật walletAddress sau khi lô được tạo
        User shop = null;
        if (batch.getShop() != null) {
            Long shopId = batch.getShop().getId();
            // Dùng EntityManager.createQuery để force load fresh từ DB, không qua L1/L2 cache
            try {
                shop = (User) entityManager.createQuery(
                        "SELECT u FROM User u WHERE u.id = :id")
                        .setParameter("id", shopId)
                        .setHint("jakarta.persistence.cache.retrieveMode", "BYPASS")
                        .getSingleResult();
            } catch (Exception e) {
                shop = userRepository.findById(shopId).orElse(null);
            }
            // Nếu walletAddress vẫn null sau khi query fresh → evict khỏi cache rồi thử lại
            if (shop != null && (shop.getWalletAddress() == null || shop.getWalletAddress().isBlank())) {
                entityManager.detach(shop);
                shop = userRepository.findById(shopId).orElse(null);
            }
        }
        
        long tokenAmount = batch.getTokenPerPackage();

        if (citizen.getWalletAddress() == null || citizen.getWalletAddress().isBlank()) {
            throw new IllegalArgumentException("Lỗi: Người dân này chưa có thông tin ví Blockchain trên hệ thống.");
        }
        if (shop == null) {
            throw new IllegalArgumentException("Lỗi hệ thống: Lô hàng #" + batchId + " chưa được gán Cửa hàng (Shop is null).");
        }
        if (shop.getWalletAddress() == null || shop.getWalletAddress().isBlank()) {
            throw new IllegalArgumentException("Lỗi: Cửa hàng " + shop.getFullName() + " (ID: " + shop.getId() + ") chưa được thiết lập địa chỉ ví Blockchain.");
        }

        // Gọi Smart Contract: Giai đoạn 2 (deliverBatch - Atomic Escrow)
        // Nếu lỗi, BlockchainException sẽ ném ra và @Transactional sẽ ROLLBACK toàn bộ DB bên dưới
        String txHash = blockchainService.deliverBatch(
                batch.getProvince(),
                citizen.getWalletAddress(),
                shop.getWalletAddress(),
                BigInteger.valueOf(tokenAmount)
        );

        log.info("Khớp lệnh Web3 Thành công (deliverBatch). Tỉnh: {}, Mức: {}, TX: {}", batch.getProvince(), tokenAmount, txHash);

        // ── Tính shopPrice thực tế từ kho của shop ────────────────────────────
        // Lấy giá shop thấp nhất trong các vật phẩm của lô (nếu có nhiều item, dùng tổng shopPrice)
        long shopPriceActual = resolveShopPrice(batch, shop.getId(), tokenAmount);
        long surplusAmount = tokenAmount - shopPriceActual; // phần dư hoàn về quỹ tỉnh

        // ── Log 1: RECEIVE_RELIEF — Hệ thống → Dân (nhận viện trợ) ──────────
        txRepository.save(new TransactionHistory(
                null,
                citizen.getId(),
                TransactionHistory.TxType.RECEIVE_RELIEF,
                tokenAmount,
                "Nhận viện trợ từ Quỹ " + batch.getProvince() + " — Lô #" + batchId,
                txHash,
                batchId
        ));

        // ── Log 2: PAY_SHOP — Dân → Shop (thanh toán phần shop) ──────────────
        txRepository.save(new TransactionHistory(
                citizen.getId(),
                shop.getId(),
                TransactionHistory.TxType.PAY_SHOP,
                shopPriceActual,
                "Thanh toán hàng cứu trợ tại Shop — Lô #" + batchId,
                txHash,
                batchId
        ));

        // ── Log 3 (nếu có dư): RETURN_SURPLUS — Dân → Quỹ tỉnh ──────────────
        if (surplusAmount > 0) {
            // Cập nhật quỹ tỉnh trong DB
            campaignPoolRepository.findByProvince(batch.getProvince()).ifPresent(pool -> {
                pool.setTotalFund(pool.getTotalFund() + surplusAmount);
                pool.setUpdatedAt(LocalDateTime.now());
                campaignPoolRepository.save(pool);
            });
            txRepository.save(new TransactionHistory(
                    citizen.getId(),
                    null, // to = System (Province Pool)
                    TransactionHistory.TxType.RETURN_SURPLUS,
                    surplusAmount,
                    "Hoàn phần dư về Quỹ " + batch.getProvince() + " — Lô #" + batchId
                    + " (trần " + tokenAmount + " - shop " + shopPriceActual + ")",
                    txHash,
                    batchId
            ));
            log.info("Hoàn surplus {} token về quỹ tỉnh {} — Lô #{}", surplusAmount, batch.getProvince(), batchId);
        }

        // Cập nhật delivered_count
        int newCount = batch.getDeliveredCount() + 1;
        batch.setDeliveredCount(newCount);
        batch.setStatus(newCount >= batch.getTotalPackages()
                ? ReliefBatchStatus.COMPLETED
                : ReliefBatchStatus.IN_PROGRESS);
        batch.setUpdatedAt(LocalDateTime.now());

        return ReliefBatchResponse.from(batchRepository.save(batch));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Tính tổng shopPrice thực tế của shop cho 1 phần trong lô.
     * Nếu shop có giá thấp hơn giá trần → trả về shopPrice, phần dư hoàn về quỹ.
     * Nếu không tìm thấy ShopItem → dùng tokenPerPackage (không có dư).
     */
    private long resolveShopPrice(ReliefBatch batch, Long shopId, long tokenPerPackage) {
        List<BatchItem> batchItems = batch.getBatchItems();
        if (batchItems == null || batchItems.isEmpty()) {
            // Legacy single-item: tìm shopItem tương ứng
            if (batch.getItem() != null) {
                Long itemId = batch.getItem().getId();
                return shopItemRepository.findByShopIdAndStatus(shopId, ShopItemStatus.ACTIVE)
                        .stream()
                        .filter(si -> si.getItem().getId().equals(itemId))
                        .findFirst()
                        .map(si -> si.getShopPrice().longValue())
                        .orElse(tokenPerPackage);
            }
            return tokenPerPackage;
        }

        // Multi-item combo: tổng (shopPrice × quantity) cho từng item
        List<com.cuutrominhbach.entity.ShopItem> shopInventory =
                shopItemRepository.findByShopIdAndStatus(shopId, ShopItemStatus.ACTIVE);

        long totalShopPrice = 0;
        for (BatchItem bi : batchItems) {
            Long itemId = bi.getItem().getId();
            long shopItemPrice = shopInventory.stream()
                    .filter(si -> si.getItem().getId().equals(itemId))
                    .findFirst()
                    .map(si -> si.getShopPrice().longValue())
                    .orElse(bi.getItem().getPriceTokens()); // fallback: giá trần
            totalShopPrice += shopItemPrice * bi.getQuantity();
        }
        // Không được vượt quá tokenPerPackage (giá trần)
        return Math.min(totalShopPrice, tokenPerPackage);
    }

    // ── TNV: Trả lô về Shop khi dân không đến nhận đủ ────────────────────────

    @Transactional
    public ReliefBatchResponse returnBatchToShop(Long batchId, Long transporterId) {
        ReliefBatch batch = getBatchOrThrow(batchId);

        if (batch.getStatus() != ReliefBatchStatus.PICKED_UP
                && batch.getStatus() != ReliefBatchStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                "Chỉ có thể trả lô đang ở trạng thái PICKED_UP hoặc IN_PROGRESS");
        }
        if (batch.getTransporter() == null || !batch.getTransporter().getId().equals(transporterId)) {
            throw new IllegalArgumentException("Bạn không phải TNV của lô này");
        }

        int remaining = batch.getTotalPackages() - batch.getDeliveredCount();
        if (remaining <= 0) {
            throw new IllegalArgumentException("Lô đã phân phát hết, không cần trả");
        }

        // Ghi log: hoàn phần chưa phân phát về quỹ tỉnh
        long refundAmount = (long) remaining * batch.getTokenPerPackage();
        campaignPoolRepository.findByProvince(batch.getProvince()).ifPresent(pool -> {
            pool.setTotalFund(pool.getTotalFund() + refundAmount);
            pool.setUpdatedAt(LocalDateTime.now());
            campaignPoolRepository.save(pool);
        });

        txRepository.save(new TransactionHistory(
                null, null,
                TransactionHistory.TxType.ALLOCATE_ESCROW, // dùng lại type này để ghi nhận hoàn quỹ
                refundAmount,
                "Hoàn " + remaining + " phần chưa phân phát về Quỹ " + batch.getProvince()
                + " — Lô #" + batchId + " (TNV trả về Shop)",
                null,
                batchId
        ));

        batch.setStatus(ReliefBatchStatus.RETURNED_TO_SHOP);
        batch.setUpdatedAt(LocalDateTime.now());
        log.info("Lô #{} được TNV {} trả về Shop. Còn {} phần chưa giao, hoàn {} token về quỹ.",
                batchId, transporterId, remaining, refundAmount);
        return ReliefBatchResponse.from(batchRepository.save(batch));
    }

    public List<com.cuutrominhbach.dto.response.TransactionResponse> getBatchTransactions(Long batchId) {
        getBatchOrThrow(batchId); // validate tồn tại
        return txRepository.findByBatchIdOrderByCreatedAtDesc(batchId)
                .stream()
                .map(com.cuutrominhbach.dto.response.TransactionResponse::from)
                .collect(Collectors.toList());
    }

    private ReliefBatch getBatchOrThrow(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô cứu trợ #" + id));
    }

    private void validateShopOwnership(ReliefBatch batch, Long shopId) {
        if (batch.getShop() == null || !batch.getShop().getId().equals(shopId)) {
            throw new IllegalArgumentException("Lô này không thuộc Shop của bạn");
        }
    }
}