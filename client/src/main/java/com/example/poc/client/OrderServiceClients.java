package com.example.poc.client;

import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.example.poc.client.error.ProblemDetailResponseErrorHandler;
import com.example.poc.service.OrderService;

/**
 * {@link OrderService} のHTTP Interfaceクライアント生成を集約するファクトリです。
 * <p>
 * PoC では Spring の Bean 定義までは持ち込まず、
 * 「必要な場所でベースURLを渡してクライアントを作る」形に寄せています。
 * その代わり、HTTP エラー変換だけは毎回同じ設定にしたいため、
 * {@link RestClient} の生成処理をこのクラスへ集約しています。
 * </p>
 * <p>
 * このクラスの責務は次の2点です。
 * </p>
 * <ul>
 * <li>{@link RestClient} に共通のエラーハンドラを設定すること</li>
 * <li>{@link HttpServiceProxyFactory} で {@code @HttpExchange} インタフェースを実体化すること</li>
 * </ul>
 */
public final class OrderServiceClients {

    private OrderServiceClients() {
    }

    /**
     * 業務例外変換込みの {@link RestClient.Builder} を返します。
     * <p>
     * ここで設定している {@link ProblemDetailResponseErrorHandler} により、
     * サーバが返した 4xx / 5xx は Spring 標準の HTTP 例外を経由して
     * {@code RemoteApiException} へ統一変換されます。
     * </p>
     *
     * @param baseUrl APIベースURL
     * @return 設定済みBuilder
     */
    public static RestClient.Builder builder(String baseUrl) {
        Assert.hasText(baseUrl, "baseUrl must not be blank");
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultStatusHandler(new ProblemDetailResponseErrorHandler());
    }

    /**
     * ベースURLから {@link OrderService} クライアントを生成します。
     *
     * @param baseUrl APIベースURL
     * @return HTTP Interfaceクライアント
     */
    public static OrderService create(String baseUrl) {
        return create(builder(baseUrl));
    }

    /**
     * 設定済みBuilderから {@link OrderService} クライアントを生成します。
     * <p>
     * 呼び出し側に見せたいのは最終的に {@link OrderService} だけなので、
     * {@link RestClient} / {@link HttpServiceProxyFactory} の組み立てはここで閉じ込めます。
     * </p>
     *
     * @param builder 設定済みBuilder
     * @return HTTP Interfaceクライアント
     */
    public static OrderService create(RestClient.Builder builder) {
        Assert.notNull(builder, "builder must not be null");
        RestClient restClient = builder.build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(OrderService.class);
    }
}
