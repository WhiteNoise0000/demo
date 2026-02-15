package com.example.poc.old.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * 旧実装側の注文エンティティです。
 * <p>
 * `java.util.Date` や `javax.persistence` など、当時一般的だった型とAPIを
 * 意図的に使って現行実装との差分を見やすくしています。
 * </p>
 */
@Entity
@Table(name = "legacy_purchase_orders")
public class LegacyPurchaseOrder {

    @Id
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    /**
     * Hibernate が利用するデフォルトコンストラクタです。
     */
    public LegacyPurchaseOrder() {
    }

    /**
     * @param id 注文ID
     * @param customerName 顧客名
     * @param status 注文状態
     * @param total 注文合計金額
     * @param createdAt 作成日時
     */
    public LegacyPurchaseOrder(Long id, String customerName, String status, BigDecimal total, Date createdAt) {
        this.id = id;
        this.customerName = customerName;
        this.status = status;
        this.total = total;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
