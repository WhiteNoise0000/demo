package com.example.poc.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;
import com.example.poc.service.OrderService;

public class ClientApplication {

    public static void main(String[] args) {
        String baseUrl = resolveBaseUrl(args);
        // RestClient + HttpServiceProxyFactory で @HttpExchange インタフェースを実体化
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        OrderService client = factory.createClient(OrderService.class);

        // 動作確認用の検索条件
        SearchCond cond = new SearchCond();
        cond.setCustomerNameKeyword("a");
        cond.setStatuses(Set.of("PAID", "SHIPPED"));
        cond.setCreatedAtFrom(Instant.parse("2026-01-01T00:00:00Z"));
        cond.setTotalGte(new BigDecimal("100.00"));

        List<OrderSearchView> specRows = client.searchSpecBySpringData(cond);
        List<OrderSearchView> criteriaRows = client.searchCriteriaByEntityManager(cond);
        List<OrderSearchView> namedJpqlRows = client.namedJpqlByEntityManager("ali");
        List<OrderStatusSummary> nativeRows = client.namedNativeByEntityManager(null);
        List<OrderStatusSummary> tupleTransformerRows = client.summaryByStatusTupleTransformer(null);

        System.out.println("baseUrl=" + baseUrl);
        System.out.println("--- search/spec ---");
        specRows.forEach(System.out::println);
        System.out.println("--- search/criteria ---");
        criteriaRows.forEach(System.out::println);
        System.out.println("--- named/jpql ---");
        namedJpqlRows.forEach(System.out::println);
        System.out.println("--- named/native ---");
        nativeRows.forEach(System.out::println);
        System.out.println("--- summary/tuple-transformer ---");
        tupleTransformerRows.forEach(System.out::println);
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
