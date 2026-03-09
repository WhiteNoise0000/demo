package com.example.poc.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.web.client.ResourceAccessException;

import com.example.poc.client.error.RemoteApiException;
import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;
import com.example.poc.service.OrderService;

/**
 * PoC 用のCLIエントリポイントです。
 * <p>
 * 本クラスでは、{@code @HttpExchange} クライアントの利用例だけでなく、
 * 例外の受け方も確認できるようにしています。
 * </p>
 * <ul>
 * <li>業務エラー: {@link RemoteApiException}</li>
 * <li>接続不可やソケットタイムアウトなどの通信障害: {@link ResourceAccessException}</li>
 * </ul>
 */
public class ClientApplication {

    public static void main(String[] args) {
        String baseUrl = resolveBaseUrl(args);
        // PoC では Bean 管理せず、その場で生成するシンプルな利用形態にしている。
        OrderService client = OrderServiceClients.create(baseUrl);

        try {
            // 動作確認用の検索条件
            SearchCond cond = new SearchCond();
            cond.setCustomerNameKeyword("a");
            cond.setStatuses(Set.of("PAID", "SHIPPED"));
            cond.setCreatedAtFrom(Instant.parse("2026-01-01T00:00:00Z"));
            cond.setTotalGte(new BigDecimal("100.00"));

            List<OrderSearchView> specRows = client.searchSpecBySpringData(cond);
            List<OrderSearchView> criteriaRows = client.searchCriteriaByEntityManager(cond);
            List<OrderSearchView> myBatisRows = client.searchDynamicByMyBatis(cond);
            List<OrderSearchView> namedJpqlRows = client.namedJpqlByEntityManager("ali");
            List<OrderStatusSummary> nativeRows = client.namedNativeByEntityManager(null);
            List<OrderStatusSummary> tupleTransformerRows = client.summaryByStatusTupleTransformer(null);
            List<OrderStatusSummary> myBatisSummaryRows = client.summaryByStatusMyBatis("PAID");

            System.out.println("baseUrl=" + baseUrl);
            System.out.println("--- search/spec ---");
            specRows.forEach(System.out::println);
            System.out.println("--- search/criteria ---");
            criteriaRows.forEach(System.out::println);
            System.out.println("--- search/dynamic/mybatis ---");
            myBatisRows.forEach(System.out::println);
            System.out.println("--- named/jpql ---");
            namedJpqlRows.forEach(System.out::println);
            System.out.println("--- named/native ---");
            nativeRows.forEach(System.out::println);
            System.out.println("--- summary/tuple-transformer ---");
            tupleTransformerRows.forEach(System.out::println);
            System.out.println("--- summary/mybatis (PAID) ---");
            myBatisSummaryRows.forEach(System.out::println);
        } catch (RemoteApiException ex) {
            // サーバが ProblemDetail を返した場合は、この1例外に集約される。
            System.err.println("Remote API error");
            System.err.println("  status=" + ex.getStatusCode().value());
            System.err.println("  code=" + ex.getErrorCode());
            System.err.println("  retryable=" + ex.isRetryable());
            System.err.println("  title=" + ex.getTitle());
            System.err.println("  detail=" + ex.getDetail());
            System.err.println("  path=" + Objects.toString(ex.getPath(), "N/A"));
        } catch (ResourceAccessException ex) {
            // DNS 失敗、接続拒否、読み取りタイムアウトなどは別系統で捕捉する。
            System.err.println("Remote API is unreachable: " + ex.getMessage());
        }
    }

    private static String resolveBaseUrl(String[] args) {
        // 優先順位: 引数 > 環境変数 POC_BASE_URL > デフォルト
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return args[0];
        }
        String fromEnv = System.getenv("POC_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return "http://localhost:8080";
    }
}
