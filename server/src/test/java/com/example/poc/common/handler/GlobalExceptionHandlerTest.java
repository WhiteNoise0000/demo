package com.example.poc.common.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link GlobalExceptionHandler} の例外マッピング検証テストです。
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /**
     * コントローラと例外ハンドラのみを組み合わせた軽量MockMvcを初期化します。
     */
    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 楽観排他例外が 409 / OPTIMISTIC_LOCK_CONFLICT に変換されることを確認します。
     */
    @Test
    void shouldMapOptimisticLockTo409() throws Exception {
        mockMvc.perform(get("/poc/errors/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
    }

    /**
     * クエリタイムアウトが 504 / DB_TIMEOUT に変換されることを確認します。
     */
    @Test
    void shouldMapQueryTimeoutTo504() throws Exception {
        mockMvc.perform(get("/poc/errors/query-timeout"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("DB_TIMEOUT"));
    }

    /**
     * DB接続不可が 503 / DB_UNAVAILABLE に変換されることを確認します。
     */
    @Test
    void shouldMapConnectionFailureTo503() throws Exception {
        mockMvc.perform(get("/poc/errors/connection"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DB_UNAVAILABLE"));
    }

    /**
     * 想定外例外が 500 / INTERNAL_ERROR に変換されることを確認します。
     */
    @Test
    void shouldMapUnexpectedTo500() throws Exception {
        mockMvc.perform(get("/poc/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    /**
     * 各種例外を意図的に送出するテスト用コントローラです。
     */
    @RestController
    static class ThrowingController {

        @GetMapping("/poc/errors/optimistic-lock")
        public String optimisticLock() {
            throw new OptimisticLockingFailureException("row version mismatch");
        }

        @GetMapping("/poc/errors/query-timeout")
        public String queryTimeout() {
            throw new QueryTimeoutException("query timeout");
        }

        @GetMapping("/poc/errors/connection")
        public String connectionFailure() {
            throw new CannotGetJdbcConnectionException("db down");
        }

        @GetMapping("/poc/errors/unexpected")
        public String unexpected() {
            throw new IllegalStateException("unexpected");
        }
    }
}
