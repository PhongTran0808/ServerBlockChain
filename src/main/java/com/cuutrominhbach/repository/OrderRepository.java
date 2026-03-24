package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.Order;
import com.cuutrominhbach.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCitizenId(Long citizenId);
    List<Order> findByShopId(Long shopId);
    List<Order> findByTransporterId(Long transporterId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByShopIdAndStatus(Long shopId, OrderStatus status);
    List<Order> findByTransporterIdAndStatus(Long transporterId, OrderStatus status);
}
