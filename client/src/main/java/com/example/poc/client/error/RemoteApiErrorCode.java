package com.example.poc.client.error;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * サーバ側 {@code ProblemDetail.code} を表す列挙です。
 * <p>
 * 文字列のまま扱うこともできますが、PoC では
 * 「利用側が typo なく switch できる」ことを優先して enum 化しています。
 * </p>
 */
public enum RemoteApiErrorCode {
    BAD_REQUEST("BAD_REQUEST"),
    DB_ERROR("DB_ERROR"),
    DB_LOCK_CONFLICT("DB_LOCK_CONFLICT"),
    DB_TIMEOUT("DB_TIMEOUT"),
    DB_UNAVAILABLE("DB_UNAVAILABLE"),
    INTERNAL_ERROR("INTERNAL_ERROR"),
    OPTIMISTIC_LOCK_CONFLICT("OPTIMISTIC_LOCK_CONFLICT"),
    UNKNOWN("UNKNOWN");

    private static final Map<String, RemoteApiErrorCode> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(RemoteApiErrorCode::wireValue, Function.identity()));

    private final String wireValue;

    RemoteApiErrorCode(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    /**
     * サーバから受け取った文字列コードを enum へ変換します。
     * <p>
     * 未知のコードを例外にせず {@link #UNKNOWN} へ寄せることで、
     * クライアントとサーバの差分があっても最低限の処理継続を可能にします。
     * </p>
     *
     * @param wireValue サーバから受け取った {@code ProblemDetail.code}
     * @return 対応する enum。未定義時は {@link #UNKNOWN}
     */
    public static RemoteApiErrorCode from(String wireValue) {
        if (wireValue == null || wireValue.isBlank()) {
            return UNKNOWN;
        }
        return LOOKUP.getOrDefault(wireValue, UNKNOWN);
    }
}
