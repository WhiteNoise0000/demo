package com.example.poc.dao.mybatis;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

/**
 * MyBatis で受注検索SQLを実行するMapperです。
 * <p>
 * 動的な WHERE 句は XML 側で組み立て、Java側は条件DTOをそのまま渡します。
 * </p>
 */
@Mapper
public interface OrderSearchMyBatisMapper {

    /**
     * 条件付き検索を実行します。
     *
     * @param cond 検索条件
     * @return 条件に一致した受注一覧
     */
    List<OrderSearchView> searchOrders(SearchCond cond);

    /**
     * ステータス集計を実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    List<OrderStatusSummary> summarizeByStatus(@Param("status") String status);
}
