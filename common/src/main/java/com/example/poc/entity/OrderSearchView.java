package com.example.poc.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 検索専用ビュー `v_order_search` に対応する読み取り専用エンティティです。
 * <p>
 * 受注 + 顧客を結合した結果を1レコードとして扱い、
 * 実装パターンごとの検索結果比較をしやすくします。
 * </p>
 */
@Entity
@Table(name = "v_order_search")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderSearchView {

    @Id
    @Column(name = "order_id")
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
