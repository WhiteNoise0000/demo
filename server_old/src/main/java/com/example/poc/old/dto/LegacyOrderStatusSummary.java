package com.example.poc.old.dto;

import java.math.BigDecimal;

/**
 * 旧実装の Native SQL 集計結果を受けるDTOです。
 * <p>
 * `AliasToBeanResultTransformer` は「SQLの別名」と「JavaBeanプロパティ名」を
 * 対応づけて値を設定するため、フィールド名をSQL aliasと合わせています。
 * </p>
 */
public class LegacyOrderStatusSummary {

    private String status;
    private Long orderCount;
    private BigDecimal totalSum;

    public LegacyOrderStatusSummary() {
    }

    public String getStatus() {
        return status;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public BigDecimal getTotalSum() {
        return totalSum;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public void setTotalSum(BigDecimal totalSum) {
        this.totalSum = totalSum;
    }
}
