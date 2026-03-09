package com.example.poc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.poc.client.error.RemoteApiErrorCode;
import com.example.poc.client.error.RemoteApiException;
import com.example.poc.service.OrderService;

/**
 * HTTP Interfaceクライアントのエラー変換テストです。
 * <p>
 * PoC として読みやすくするため、ここでは「どういうレスポンスが来たら
 * クライアントから何が見えるか」を 1 ケースずつ固定化しています。
 * </p>
 */
class OrderServiceClientsTest {

    private static final String BASE_URL = "http://example.test";

    private MockRestServiceServer server;
    private OrderService client;

    @BeforeEach
    void setUp() {
        // 本番コードと同じ builder 設定を使い、エラーハンドラの差し替え漏れを防ぐ。
        RestClient.Builder builder = OrderServiceClients.builder(BASE_URL);
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.client = OrderServiceClients.create(builder);
    }

    @Test
    void shouldTranslateConflictProblemToDedicatedException() {
        // 409 + code により、呼び出し側が「競合なので再試行候補」と判断できることを確認する。
        server.expect(requestTo(BASE_URL + "/orders/named/jpql/entity-manager?kw=ali"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "about:blank",
                                  "title": "Optimistic lock conflict",
                                  "status": 409,
                                  "detail": "The target data was updated by another transaction. Retry the operation.",
                                  "code": "OPTIMISTIC_LOCK_CONFLICT",
                                  "path": "/orders/named/jpql/entity-manager"
                                }
                                """));

        assertThatThrownBy(() -> client.namedJpqlByEntityManager("ali"))
                .isInstanceOf(RemoteApiException.class)
                .satisfies(ex -> {
                    RemoteApiException apiException = (RemoteApiException) ex;
                    assertThat(apiException.getStatusCode().value()).isEqualTo(409);
                    assertThat(apiException.getErrorCode()).isEqualTo(RemoteApiErrorCode.OPTIMISTIC_LOCK_CONFLICT);
                    assertThat(apiException.isRetryable()).isTrue();
                    assertThat(apiException.getPath()).isEqualTo("/orders/named/jpql/entity-manager");
                    assertThat(apiException.getDetail())
                            .isEqualTo("The target data was updated by another transaction. Retry the operation.");
                });
    }

    @Test
    void shouldTranslateBadRequestProblemToDedicatedException() {
        // 400 は業務再試行ではなく、入力見直し側へ寄せたい。
        server.expect(requestTo(BASE_URL + "/orders/named/jpql/entity-manager?kw=ali"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "about:blank",
                                  "title": "Bad request",
                                  "status": 400,
                                  "detail": "kw must not be blank",
                                  "code": "BAD_REQUEST"
                                }
                                """));

        assertThatThrownBy(() -> client.namedJpqlByEntityManager("ali"))
                .isInstanceOf(RemoteApiException.class)
                .satisfies(ex -> {
                    RemoteApiException apiException = (RemoteApiException) ex;
                    assertThat(apiException.getErrorCode()).isEqualTo(RemoteApiErrorCode.BAD_REQUEST);
                    assertThat(apiException.isRetryable()).isFalse();
                    assertThat(apiException.getTitle()).isEqualTo("Bad request");
                });
    }

    @Test
    void shouldTranslateGatewayTimeoutProblemToDedicatedException() {
        // 504 は ProblemDetail.code から再試行候補に判定できる。
        server.expect(requestTo(BASE_URL + "/orders/summary/mybatis?status=PAID"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "about:blank",
                                  "title": "Database timeout",
                                  "status": 504,
                                  "detail": "Database operation timed out. Retry later.",
                                  "code": "DB_TIMEOUT"
                                }
                                """));

        assertThatThrownBy(() -> client.summaryByStatusMyBatis("PAID"))
                .isInstanceOf(RemoteApiException.class)
                .satisfies(ex -> {
                    RemoteApiException apiException = (RemoteApiException) ex;
                    assertThat(apiException.getErrorCode()).isEqualTo(RemoteApiErrorCode.DB_TIMEOUT);
                    assertThat(apiException.isRetryable()).isTrue();
                });
    }

    @Test
    void shouldFallbackToPlainTextBodyWhenProblemDetailIsMissing() {
        // 相手が ProblemDetail 非対応でも、同じ例外型で受けられることを確認する。
        server.expect(requestTo(BASE_URL + "/orders/named/native/entity-manager"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("database is down"));

        assertThatThrownBy(() -> client.namedNativeByEntityManager(null))
                .isInstanceOf(RemoteApiException.class)
                .satisfies(ex -> {
                    RemoteApiException apiException = (RemoteApiException) ex;
                    assertThat(apiException.getErrorCode()).isEqualTo(RemoteApiErrorCode.UNKNOWN);
                    assertThat(apiException.isRetryable()).isTrue();
                    assertThat(apiException.getDetail()).isEqualTo("database is down");
                });
    }
}
