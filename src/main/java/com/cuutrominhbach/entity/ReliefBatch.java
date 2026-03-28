package com.cuutrominhbach.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Lô Cứu Trợ — đơn vị phân bổ tập trung thay thế mô hình đặt hàng lẻ.
 * Admin tạo → TNV nhận → Shop duyệt → TNV lấy hàng → Phân phát cho dân.
 */
@Entity
@Table(name = "relief_batches")
public class ReliefBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên gói cứu trợ, VD: "Gói Sinh Tồn Đợt 1" */
    private String name;

    /** Tỉnh/Thành phố mục tiêu */
    private String province;

    /** Tổng số phần (packages) trong lô */
    @Column(name = "total_packages")
    private Integer totalPackages;

    /** Số token mỗi phần — trừ từ ví dân khi nhận */
    @Column(name = "token_per_package")
    private Long tokenPerPackage;

    /** Số phần đã giao thành công */
    @Column(name = "delivered_count")
    private Integer deliveredCount = 0;

    @Enumerated(EnumType.STRING)
    private ReliefBatchStatus status = ReliefBatchStatus.CREATED;

    /** TNV đã nhận lô này */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id")
    private User transporter;

    /** Shop được chọn để lấy hàng */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private User shop;

    /** Admin tạo lô */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** Nhu yếu phẩm trong lô (1 người nhận 1 phần của item này) — legacy, dùng batchItems thay thế */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id")
    private Item item;

    /** Danh sách vật phẩm combo trong lô */
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private java.util.List<BatchItem> batchItems = new java.util.ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ReliefBatch() {}

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getProvince() { return province; }
    public Integer getTotalPackages() { return totalPackages; }
    public Long getTokenPerPackage() { return tokenPerPackage; }
    public Integer getDeliveredCount() { return deliveredCount; }
    public ReliefBatchStatus getStatus() { return status; }
    public User getTransporter() { return transporter; }
    public User getShop() { return shop; }
    public User getCreatedBy() { return createdBy; }
    public Item getItem() { return item; }
    public java.util.List<BatchItem> getBatchItems() { return batchItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setProvince(String province) { this.province = province; }
    public void setTotalPackages(Integer totalPackages) { this.totalPackages = totalPackages; }
    public void setTokenPerPackage(Long tokenPerPackage) { this.tokenPerPackage = tokenPerPackage; }
    public void setDeliveredCount(Integer deliveredCount) { this.deliveredCount = deliveredCount; }
    public void setStatus(ReliefBatchStatus status) { this.status = status; }
    public void setTransporter(User transporter) { this.transporter = transporter; }
    public void setShop(User shop) { this.shop = shop; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public void setItem(Item item) { this.item = item; }
    public void setBatchItems(java.util.List<BatchItem> batchItems) { this.batchItems = batchItems; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
