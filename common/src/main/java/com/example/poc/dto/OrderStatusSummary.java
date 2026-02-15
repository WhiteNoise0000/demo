package com.example.poc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * ステータス単位の集計結果を表すDTOです。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderStatusSummary {

    private String status;
    private Long orderCount;
    private Long totalSum;
}
