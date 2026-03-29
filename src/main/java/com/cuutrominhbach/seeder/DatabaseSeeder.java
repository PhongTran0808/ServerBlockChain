package com.cuutrominhbach.seeder;

import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.entity.TransactionHistory.TxType;
import com.cuutrominhbach.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    
    private final TransactionHistoryRepository txRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ReliefBatchRepository batchRepository;
    private final CampaignPoolRepository campaignPoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final ShopItemRepository shopItemRepository;

    public DatabaseSeeder(TransactionHistoryRepository txRepository, UserRepository userRepository,
                          ItemRepository itemRepository, ReliefBatchRepository batchRepository,
                          CampaignPoolRepository campaignPoolRepository, PasswordEncoder passwordEncoder,
                          ShopItemRepository shopItemRepository) {
        this.txRepository = txRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.campaignPoolRepository = campaignPoolRepository;
        this.passwordEncoder = passwordEncoder;
        this.shopItemRepository = shopItemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Khởi động hệ thống Audit & Tự phục hồi dữ liệu (Seeder)...");

        // 1. Phục hồi 100% Khả năng Đăng nhập cho User từ thư mục setup.sql
        fixAllPasswordsAndApprovals();

        // 2. Bơm đủ 77 Citizen theo yêu cầu nếu chưa đủ
        seedMissingCitizens();

        // 3. Khôi phục 15 Vật phẩm Cứu trợ chuẩn xác
        if (itemRepository.count() == 0) {
            seedItems();
        }

        log.info("Khởi tạo cấu trúc Quỹ cho các khu vực nếu chưa có...");
        seedCampaignPools();

        log.info("Khởi tạo 15 vật phẩm cho các Shop nếu chưa có...");
        seedShopInventory();

        log.info("Kịch bản dòng tiền Web3 hoàn tất (Bỏ qua tạo lô mới để tránh trùng lặp dữ liệu)!");
    }

    private void fixAllPasswordsAndApprovals() {
        String defaultHash = passwordEncoder.encode("123456");
        List<User> allUsers = userRepository.findAll();
        boolean updated = false;
        
        for (User u : allUsers) {
            if (!defaultHash.equals(u.getHashPassword()) || !Boolean.TRUE.equals(u.getIsApproved())) {
                u.setHashPassword(defaultHash);
                u.setIsApproved(true); // Mở khóa cho Shop & Transporter
                updated = true;
            }
        }
        
        if (updated) {
            userRepository.saveAll(allUsers);
            log.info("🔧 Đã đồng bộ lại Mật khẩu chuẩn (123456) và Mở khóa thành công cho tất cả User đang có!");
        }
    }

    private void seedCampaignPools() {
        String[] provincesToSeed = {"Hà Nội", "TP.HCM", "Đà Nẵng", "Cần Thơ", "Huế", "Đồng Nai"};
        long[] initialFunds = {5_000_000_000L, 4_000_000_000L, 3_000_000_000L, 2_500_000_000L, 2_000_000_000L, 2_200_000_000L};
        
        for (int i = 0; i < provincesToSeed.length; i++) {
            String p = provincesToSeed[i];
            Long fund = initialFunds[i];
            campaignPoolRepository.findByProvince(p).orElseGet(() -> campaignPoolRepository.save(CampaignPool.builder()
                    .province(p)
                    .campaignCode("CMP-" + p.toUpperCase().replace(" ", "_").replace(".", ""))
                    .totalFund(fund)
                    .isReceivingActive(true)
                    .updatedAt(LocalDateTime.now())
                    .build()));
        }
    }

    private void seedShopInventory() {
        List<User> shops = userRepository.findAll().stream().filter(u -> u.getRole() == Role.SHOP).toList();
        List<Item> items = itemRepository.findAll();
        
        if (shops.isEmpty() || items.isEmpty()) return;
        
        int added = 0;
        for (User shop : shops) {
            for (Item item : items) {
                // Check if shop_items already exists for this shop & item
                boolean hasItem = shopItemRepository.findByShopId(shop.getId()).stream()
                    .anyMatch(si -> si.getItem().getId().equals(item.getId()));
                
                if (!hasItem) {
                    ShopItem si = new ShopItem();
                    si.setShop(shop);
                    si.setItem(item);
                    si.setShopPrice(BigDecimal.valueOf(item.getPriceTokens())); // Default to standard item price
                    si.setQuantity(10000); // Massive stock so they don't run out
                    si.setStatus(ShopItemStatus.ACTIVE);
                    si.setCreatedAt(LocalDateTime.now());
                    si.setUpdatedAt(LocalDateTime.now());
                    shopItemRepository.save(si);
                    added++;
                }
            }
        }
        if (added > 0) {
            log.info("🛒 Đã bơm {} kho hàng mới vào hệ thống Shop (mỗi vật phẩm 10.000 món)!", added);
        }
    }


    private void seedMissingCitizens() {
        String[] mainProvinces = {"Hà Nội", "TP.HCM", "Đà Nẵng", "Cần Thơ", "Huế"};
        String defaultHash = passwordEncoder.encode("123456");

        long currentCitizenCount = userRepository.findAll().stream().filter(u -> u.getRole() == Role.CITIZEN).count();
        int target = 79; // Tăng lên 79 để bao gồm 2 người ở Đồng Nai
        
        if (currentCitizenCount < target) {
            log.info("Phát hiện hệ thống chỉ có {} Citizen. Đang bơm thêm để đạt chuẩn {} Citizen...", currentCitizenCount, target);
            for (int i = (int)currentCitizenCount + 1; i <= target; i++) {
                User cit = new User();
                cit.setUsername(String.format("00123456789%04d", i));
                cit.setFullName("Nguyễn Văn " + i);
                cit.setRole(Role.CITIZEN);
                
                // Đặc cách cho Đồng Nai (chỉ 2 người 78 & 79)
                if (i == 78 || i == 79) {
                    cit.setProvince("Đồng Nai");
                } else {
                    cit.setProvince(mainProvinces[(i - 1) % mainProvinces.length]);
                }
                
                cit.setWalletAddress("0x" + (UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")).substring(0, 40)); 
                cit.setHashPassword(defaultHash);
                cit.setIsApproved(true);
                cit.setCreatedAt(LocalDateTime.now());
                userRepository.save(cit);
            }
            log.info("✅ Đã tạo bổ sung thành công {} Người dân mới (bao gồm khu vực Đồng Nai)!", target - currentCitizenCount);
        }
    }

    private void seedItems() {
        log.info("Đang nạp dữ liệu 15 Vật phẩm Nhu yếu phẩm khẩn cấp...");
        Object[][] itemData = {
            {1L, "Gạo 5kg", "https://cdn.tgdd.vn/Products/Images/2513/223666/bhx/sellingpoint.jpg", 50000L},
            {2L, "Mì tôm thùng", "https://cdnv2.tgdd.vn/bhx-static/bhx/News/Images/2025/06/07/1578704/image1_202506071745476610.jpg", 30000L},
            {3L, "Nước uống 24 chai", "https://cdnv2.tgdd.vn/bhx-static/bhx/Products/Images/2563/84812/bhx/thung-24-chai-nuoc-khoang-la-vie-500ml_202511261110593929.jpg", 80000L},
            {4L, "Tôn lợp nhà", "https://sudospaces.com/polybinhduong-com/2024/03/ton-lop-mai.jpg", 500000L},
            {5L, "Chăn màn", "https://thuvienmuasam.com/uploads/default/original/2X/b/b4ed874a2b5b5b98a01030d2548b6fa7b4880d92.jpeg", 90000L},
            {6L, "Đèn pin", "https://static.rangdongstore.vn/product/Den-LED/LED_Den_pin/LED_D_PDD03L_5W/LED-D-PDD03L-5W.jpg?fm=webp&w=500", 20000L},
            {7L, "Túi thuốc y tế cơ bản", "https://tse3.mm.bing.net/th/id/OIP.MoGQprdSynylPYxy7avy1QHaHa?rs=1&pid=ImgDetMain&o=7&rm=3", 90000L},
            {8L, "Lương khô", "https://tse3.mm.bing.net/th/id/OIP.C_MNm9YbHSdruQDzNXXiRwHaEK?rs=1&pid=ImgDetMain&o=7&rm=3", 22000L},
            {9L, "Sữa thùng", "https://down-vn.img.susercontent.com/file/vn-11134207-7r98o-llda7mtkm3li93", 80000L},
            {10L, "Áo mưa", "https://vandacenter.vn/wp-content/uploads/2020/11/ao-mua-tai-da-nang.jpg", 10000L},
            {11L, "Giấy vệ sinh (10 cuộn)", "https://down-vn.img.susercontent.com/file/vn-11134207-7qukw-lgibwvt9xh0q20", 35000L},
            {12L, "Bật lửa", "https://tse3.mm.bing.net/th/id/OIP.IfxyVh2HWC3lunVXqXoNyAHaHa?rs=1&pid=ImgDetMain&o=7&rm=3", 3000L},
            {13L, "Ủng cao su", "https://file.hstatic.net/200000427375/article/ung-cao-su-cach-dien-viet-thang__1__2f55ff7710a24b2494af9fec8e8f0fa9_grande.jpg", 10000L},
            {14L, "Áo phao cứu hộ", "https://tse4.mm.bing.net/th/id/OIP.Xp2RfTU8FCFNauaEYvdFCwHaHa?w=500&h=500&rs=1&pid=ImgDetMain&o=7&rm=3", 20000L},
            {15L, "Sạc dự phòng", "https://choetechofficial.com.vn/wp-content/uploads/2022/11/Pin-sac-du-phong-10000mAh-PD-18W-di-dong-QC-3.0-Bo-pin-ngoai-Power-Bank-USB-C-CHOETECH-ma-B627-9.jpg", 100000L}
        };

        for (Object[] row : itemData) {
            Item it = new Item();
            it.setTokenId((Long) row[0]);
            it.setName((String) row[1]);
            it.setImageUrl((String) row[2]);
            it.setStatus(ItemStatus.ACTIVE);
            it.setPriceTokens((Long) row[3]);
            itemRepository.save(it);
        }
        log.info("✅ Nạp 15 Vật phẩm thành công!");
    }

    private void executeScenario1() {
        List<User> richDonors = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CITIZEN && 
                        ("Hà Nội".equalsIgnoreCase(u.getProvince()) || "TP.HCM".equalsIgnoreCase(u.getProvince())))
                .limit(8)
                .toList();

        if (richDonors.isEmpty()) {
            User fallbackDonor = new User();
            fallbackDonor.setUsername("daigia01");
            fallbackDonor.setFullName("Tuyển Đại Gia");
            fallbackDonor.setRole(Role.CITIZEN);
            fallbackDonor.setProvince("Hà Nội");
            fallbackDonor.setHashPassword(passwordEncoder.encode("123456"));
            fallbackDonor.setIsApproved(true);
            fallbackDonor.setCreatedAt(LocalDateTime.now());
            fallbackDonor = userRepository.save(fallbackDonor);
            richDonors = List.of(fallbackDonor);
        }

        // Khởi tạo Quỹ cho TẤT CẢ 6 tỉnh để hiển thị trên UI
        String[] provincesToSeed = {"Hà Nội", "TP.HCM", "Đà Nẵng", "Cần Thơ", "Huế", "Đồng Nai"};
        for (String p : provincesToSeed) {
            campaignPoolRepository.findByProvince(p).orElseGet(() -> campaignPoolRepository.save(CampaignPool.builder()
                    .province(p)
                    .campaignCode("CMP-" + p.toUpperCase().replace(" ", "_").replace(".", ""))
                    .totalFund(0L)
                    .isReceivingActive(true)
                    .updatedAt(LocalDateTime.now())
                    .build()));
        }

        CampaignPool canThoPool = campaignPoolRepository.findByProvince("Cần Thơ").orElseThrow();

        long totalDonated = 0;

        for (User donor : richDonors) {
            long amount = 50_000_000L + (long)(Math.random() * 1_950_000_000L);
            totalDonated += amount;

            // Bơm sẵn tiền vào ví của Đại gia (nhiều hơn số tiền họ sắp quyên góp) để tránh ví bị Âm
            TransactionHistory txIn = new TransactionHistory(
                    null, donor.getId(), 
                    TxType.IN, 
                    amount + 100_000_000L, 
                    "Nạp tiền từ ngân hàng (Hệ thống giả lập)", 
                    generateFakeTxHash(), null
            );
            txIn.setCreatedAt(LocalDateTime.now().minusDays(10).minusHours((long)(Math.random() * 24)));
            txRepository.save(txIn);

            TransactionHistory txDonate = new TransactionHistory(
                    donor.getId(), null, 
                    TxType.DONATE, 
                    amount, 
                    "Nhà hảo tâm " + donor.getFullName() + " (" + donor.getProvince() + ") quyên góp", 
                    generateFakeTxHash(), null
            );
            txDonate.setCreatedAt(LocalDateTime.now().minusDays(5).minusHours((long)(Math.random() * 24))); 
            txRepository.save(txDonate);
        }

        canThoPool.setTotalFund(canThoPool.getTotalFund() + totalDonated);
        campaignPoolRepository.save(canThoPool);
        log.info("=> Kịch Bản 1 Xong: Đã bơm {} Token vào Quỹ Cần Thơ từ {} Đại gia", totalDonated, richDonors.size());
    }

    private void executeScenario2() {
        String targetProvince = "Cần Thơ";
        User shop = findOrInsertUser(Role.SHOP, targetProvince, "shop_cantho_99", "Cửa hàng KKB2");
        User transporter = findOrInsertUser(Role.TRANSPORTER, targetProvince, "trans_cantho_99", "TNV KKB2");
        User citizen = findOrInsertUser(Role.CITIZEN, targetProvince, "cit_cantho_99", "Người Dân KKB2");
        
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN).findFirst()
                .orElseGet(() -> findOrInsertUser(Role.ADMIN, "Hệ Thống", "admin_kkb2", "Admin KKB2"));

        Item itemMock = itemRepository.findById(1L).orElse(itemRepository.findAll().get(0));

        long tokenAmount = 5_000_000L; 
        ReliefBatch batch = new ReliefBatch();
        batch.setName("Lô Hàng Cứu Trợ VIP - KKB2");
        batch.setProvince(targetProvince);
        batch.setTotalPackages(1);
        batch.setTokenPerPackage(tokenAmount);
        batch.setDeliveredCount(1);
        batch.setStatus(ReliefBatchStatus.COMPLETED);
        batch.setCreatedBy(admin);
        batch.setShop(shop);
        batch.setTransporter(transporter);
        batch.setCreatedAt(LocalDateTime.now().minusHours(24));
        batch.setUpdatedAt(LocalDateTime.now().minusHours(1));
        
        batch.getBatchItems().add(new BatchItem(batch, itemMock, 1));
        ReliefBatch savedBatch = batchRepository.save(batch);

        Long batchId = savedBatch.getId();
        
        txRepository.save(new TransactionHistory(
                null, null, // Không đích danh Huỳnh Thanh Tú nữa mà là Khóa Quỹ Hệ thống
                TxType.ALLOCATE_ESCROW, tokenAmount, 
                "Khóa quỹ (Quỹ Cần Thơ) tạo lô cứu trợ KKB2", 
                generateFakeTxHash(), batchId
        ));

        String atomicTxHash = generateFakeTxHash();

        txRepository.save(new TransactionHistory(
                null, citizen.getId(), 
                TxType.RECEIVE_RELIEF, tokenAmount, 
                "Người dân nhận 5.000.000 Token từ Quỹ", 
                atomicTxHash, batchId
        ));

        // Dòng số 3 (cùng atomicTxHash)
        txRepository.save(new TransactionHistory(
                citizen.getId(), shop.getId(), 
                TxType.PAY_SHOP, tokenAmount, 
                "Thanh toán ngay lập tức 5.000.000 Token cho Shop KKB2", 
                atomicTxHash, batchId
        ));

        log.info("=> Kịch Bản 2 Xong: Đã tạo và quét QR Lô hàng Cần Thơ với 100% Nguyên Tử On-chain!");
    }

    private User findOrInsertUser(Role role, String province, String mockUsername, String mockFullName) {
        Optional<User> existing = userRepository.findAll().stream()
                .filter(u -> u.getRole() == role && province.equalsIgnoreCase(u.getProvince()))
                .findFirst();
        
        if (existing.isPresent()) {
            return existing.get();
        }

        User u = new User();
        u.setUsername(mockUsername);
        u.setFullName(mockFullName);
        u.setRole(role);
        u.setProvince(province);
        u.setHashPassword(passwordEncoder.encode("123456"));
        u.setIsApproved(true);
        u.setWalletAddress(generateFakeTxHash().substring(0, 42)); 
        u.setCreatedAt(LocalDateTime.now());
        log.warn("Fallback KKB2: Đã thêm tay user [{}]", mockUsername);
        return userRepository.save(u);
    }

    private String generateFakeTxHash() {
        return "0x" + UUID.randomUUID().toString().replace("-", "") 
                  + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}
