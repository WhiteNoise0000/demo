package com.example.poc.dao.spec;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

/**
 * `SearchCond` を Spring Data Specification へ変換するファクトリです。
 * <p>
 * 条件部品を小さなメソッドに分解し、どの条件をどう合成したかを読みやすくしています。
 * </p>
 */
public final class OrderSearchSpecificationFactory {

    private OrderSearchSpecificationFactory() {
    }

    /**
     * 顧客名の大文字小文字を無視した部分一致条件を返します。
     *
     * @param keyword 部分一致キーワード
     * @return 顧客名条件
     */
    public static Specification<OrderSearchView> customerNameContainsIgnoreCase(String keyword) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("customerName")), "%" + keyword.toLowerCase() + "%");
    }

    /**
     * ステータスのIN条件を返します。
     *
     * @param statuses ステータス集合
     * @return ステータス条件
     */
    public static Specification<OrderSearchView> statusIn(Set<String> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /**
     * 作成日時の下限条件を返します。
     *
     * @param from 下限日時
     * @return 作成日時の下限条件
     */
    public static Specification<OrderSearchView> createdAtFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    /**
     * 作成日時の上限条件を返します。
     *
     * @param to 上限日時
     * @return 作成日時の上限条件
     */
    public static Specification<OrderSearchView> createdAtTo(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    /**
     * 合計金額の下限条件を返します。
     *
     * @param minTotal 下限金額
     * @return 合計金額の下限条件
     */
    public static Specification<OrderSearchView> totalGte(BigDecimal minTotal) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("total"), minTotal);
    }

    /**
     * 入力条件から動的WHERE句を組み立てます。
     *
     * @param cond 検索条件（nullの場合は全件）
     * @return 合成済みSpecification
     */
    public static Specification<OrderSearchView> fromCondition(SearchCond cond) {
        Specification<OrderSearchView> spec = (root, query, cb) -> cb.conjunction();
        if (cond == null) {
            return spec;
        }

        if (cond.getCustomerNameKeyword() != null && !cond.getCustomerNameKeyword().isBlank()) {
            spec = spec.and(customerNameContainsIgnoreCase(cond.getCustomerNameKeyword()));
        }
        if (cond.getStatuses() != null && !cond.getStatuses().isEmpty()) {
            spec = spec.and(statusIn(cond.getStatuses()));
        }
        if (cond.getCreatedAtFrom() != null) {
            spec = spec.and(createdAtFrom(cond.getCreatedAtFrom()));
        }
        if (cond.getCreatedAtTo() != null) {
            spec = spec.and(createdAtTo(cond.getCreatedAtTo()));
        }
        if (cond.getTotalGte() != null) {
            spec = spec.and(totalGte(cond.getTotalGte()));
        }
        return spec;
    }
}
