package com.cuutrominhbach.entity;

public enum OrderStatus {
    PENDING,
    READY,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED,
    REFUNDED_LOST   // TNV làm mất hàng — citizen được hoàn tiền, shop được bồi thường
}
