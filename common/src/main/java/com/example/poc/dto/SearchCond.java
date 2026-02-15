package com.example.poc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 受注検索APIの入力条件DTOです。
 * <p>
 * どの実装パターンでも同じ条件モデルを使うことで、
 * 返却結果や可読性の差分だけを比較できるようにしています。
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class SearchCond {

    private String customerNameKeyword;
    private Set<String> statuses;
    private Instant createdAtFrom;
    private Instant createdAtTo;
    private BigDecimal totalGte;
}
