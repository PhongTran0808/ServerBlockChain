package com.cuutrominhbach.entity;

public enum ReliefBatchStatus {
    CREATED,        // Admin tạo, chờ TNV nhận
    WAITING_SHOP,   // TNV đã nhận, chờ Shop duyệt
    SHOP_REJECTED,  // Shop từ chối
    ACCEPTED,       // Shop chấp nhận, chờ TNV đến lấy
    PICKED_UP,      // TNV đã lấy hàng tại Shop
    IN_PROGRESS,    // Đang phân phát cho dân
    COMPLETED       // Đã phân phát hết
}
