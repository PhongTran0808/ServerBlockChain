package com.cuutrominhbach.service;

import com.cuutrominhbach.blockchain.BlockchainService;
import com.cuutrominhbach.dto.request.ConfirmDeliveryRequest;
import com.cuutrominhbach.dto.request.CreateOrderRequest;
import com.cuutrominhbach.dto.request.OfflineQueueItem;
import com.cuutrominhbach.dto.response.OrderResponse;
import com.cuutrominhbach.entity.*;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.exception.InsufficientTokenException;
import com.cuutrominhbach.repository.ItemRepository;
import com.cuutrominhbach.repository.OrderRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EscrowService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BlockchainService blockchainService;
    private final PasswordEncoder passwordEncoder;

    public EscrowService(OrderRepository orderRepository,
                         ItemRepository itemRepository,
                         UserRepository userRepository,
                         BlockchainService blockchainService,
                         PasswordEncoder passwordEncoder) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.blockchainService = blockchainService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OrderResponse createOrder(Long citizenId, CreateOrderRequest req) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (citizen.getRole() != Role.CITIZEN) {
            throw new IllegalArgumentException("Chỉ CITIZEN mới có thể tạo đơn hàng");
        }

        // Verify PIN
        if (!passwordEncoder.matches(req.getPin(), citizen.getHashPassword())) {
            throw new AuthException("PIN không đúng");
        }

        Item item = itemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vật phẩm"));

        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new IllegalArgumentException("Vật phẩm không còn khả dụng");
        }

        User shop = userRepository.findById(req.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng"));

        if (!Boolean.TRUE.equals(shop.getIsApproved())) {
            throw new IllegalArgumentException("Cửa hàng chưa được admin phê duyệt");
        }

        // Lock tokens on blockchain
        String lockTxHash = blockchainService.lockTokens(
                citizen.getWalletAddress(),
                BigInteger.valueOf(0), // orderId placeholder, will update after save
                BigInteger.valueOf(item.getPriceTokens())
        );

        Order order = Order.builder()
                .citizen(citizen)
                .shop(shop)
                .status(OrderStatus.PENDING)
                .totalTokens(item.getPriceTokens())
                .lockTxHash(lockTxHash)
                .itemId(item.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse confirmDelivery(Long orderId, ConfirmDeliveryRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        User citizen = order.getCitizen();
        if (citizen == null) {
            throw new IllegalArgumentException("Đơn hàng không có thông tin người nhận");
        }

        // Verify citizen PIN
        if (!passwordEncoder.matches(req.getCitizenPin(), citizen.getHashPassword())) {
            throw new AuthException("PIN không đúng");
        }

        // Release tokens on blockchain
        String releaseTxHash = blockchainService.releaseTokens(BigInteger.valueOf(orderId));

        order.setStatus(OrderStatus.DELIVERED);
        order.setReleaseTxHash(releaseTxHash);
        order.setUpdatedAt(LocalDateTime.now());

        order = orderRepository.save(order);
        return OrderResponse.from(order);
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
                // Log and skip failed items
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
        if (shop.getRole() != Role.SHOP) {
            throw new IllegalArgumentException("Chỉ SHOP mới được đánh dấu đơn READY");
        }
        if (!Boolean.TRUE.equals(shop.getIsApproved())) {
            throw new IllegalArgumentException("Cửa hàng chưa được admin phê duyệt");
        }

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

        if (!Boolean.TRUE.equals(transporter.getIsApproved())) {
            throw new IllegalArgumentException("Shipper chưa được admin phê duyệt");
        }

        if (transporter.getRole() != Role.TRANSPORTER) {
            throw new IllegalArgumentException("Chỉ TRANSPORTER mới được nhận đơn");
        }

        order.setTransporter(transporter);
        order.setStatus(OrderStatus.IN_TRANSIT);
        order.setUpdatedAt(LocalDateTime.now());
        return OrderResponse.from(orderRepository.save(order));
    }
}
