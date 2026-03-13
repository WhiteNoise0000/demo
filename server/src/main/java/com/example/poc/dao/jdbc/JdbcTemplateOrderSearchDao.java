package com.example.poc.dao.jdbc;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

/**
 * NamedParameterJdbcTemplate を使う SQL-first な検索 DAO です。
 * <p>
 * 長い SQL は text block で保持し、必要な条件だけ Java 側で組み立てます。
 * </p>
 */
@Repository
public class JdbcTemplateOrderSearchDao extends JdbcDaoBase {

    private static final String SEARCH_SELECT = """
            select
                o.id as id,
                c.name as customer_name,
                o.status as status,
                o.total as total,
                o.created_at as created_at
            from purchase_orders o
            join customers c on c.id = o.customer_id
            """;

    private static final String SUMMARY_SQL = """
            select
                status as status,
                cast(count(*) as bigint) as order_count,
                cast(sum(total) as bigint) as total_sum
            from purchase_orders
            where (:status is null or status = :status)
            group by status
            order by status
            """;

    public JdbcTemplateOrderSearchDao(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /**
     * 条件付き検索を実行します。
     *
     * @param cond 検索条件
     * @return 条件に一致した受注一覧
     */
    public List<OrderSearchView> searchOrders(SearchCond cond) {
        StringBuilder sql = new StringBuilder(SEARCH_SELECT);
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> predicates = new ArrayList<>();

        if (cond != null) {
            if (hasText(cond.getCustomerNameKeyword())) {
                predicates.add("lower(c.name) like concat('%', lower(:customerNameKeyword), '%')");
                params.addValue("customerNameKeyword", cond.getCustomerNameKeyword());
            }
            if (cond.getStatuses() != null && !cond.getStatuses().isEmpty()) {
                predicates.add("o.status in (:statuses)");
                params.addValue("statuses", cond.getStatuses());
            }
            if (cond.getCreatedAtFrom() != null) {
                predicates.add("o.created_at >= :createdAtFrom");
                params.addValue("createdAtFrom", cond.getCreatedAtFrom());
            }
            if (cond.getCreatedAtTo() != null) {
                predicates.add("o.created_at <= :createdAtTo");
                params.addValue("createdAtTo", cond.getCreatedAtTo());
            }
            if (cond.getTotalGte() != null) {
                predicates.add("o.total >= :totalGte");
                params.addValue("totalGte", cond.getTotalGte());
            }
        }

        if (!predicates.isEmpty()) {
            sql.append("where ");
            sql.append(String.join("\n  and ", predicates));
            sql.append('\n');
        }
        sql.append("order by o.created_at desc");

        return queryList(sql.toString(), params, OrderSearchView.class);
    }

    /**
     * ステータス単位の集計を実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    public List<OrderStatusSummary> summarizeByStatus(String status) {
        return queryList(SUMMARY_SQL, new MapSqlParameterSource("status", status), OrderStatusSummary.class);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
