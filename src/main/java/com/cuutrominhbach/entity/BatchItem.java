package com.cuutrominhbach.entity;

import jakarta.persistence.*;

/**
 * Bảng pivot batch_items — mỗi dòng là 1 vật phẩm trong 1 lô cứu trợ.
 * Một lô có thể chứa nhiều vật phẩm (combo).
 */
@Entity
@Table(name = "batch_items")
public class BatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ReliefBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** Số lượng vật phẩm này trong 1 phần (VD: 2 gói mì, 1 túi gạo) */
    @Column(nullable = false)
    private Integer quantity = 1;

    public BatchItem() {}

    public BatchItem(ReliefBatch batch, Item item, Integer quantity) {
        this.batch = batch;
        this.item = item;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public ReliefBatch getBatch() { return batch; }
    public Item getItem() { return item; }
    public Integer getQuantity() { return quantity; }

    public void setId(Long id) { this.id = id; }
    public void setBatch(ReliefBatch batch) { this.batch = batch; }
    public void setItem(Item item) { this.item = item; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
