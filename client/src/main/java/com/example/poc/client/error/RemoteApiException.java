package com.example.poc.client.error;

import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * サーバ側HTTPエラーを表すクライアント専用例外の基底です。
 * <p>
 * PoC では例外クラスを細かく増やさず、
 * 「HTTPエラーはひとまずこの1クラスで受ける」方針にしています。
 * </p>
 * <p>
 * その代わり、この例外の中へ判断材料をまとめて保持します。
 * 呼び出し側は例外の型を増やして分岐するのではなく、
 * {@link #getErrorCode()} や {@link #isRetryable()} を見て処理方針を決めます。
 * </p>
 * <ul>
 * <li>HTTP ステータス: {@link #getStatusCode()}</li>
 * <li>サーバが返した問題詳細: {@link #getProblemDetail()}</li>
 * <li>アプリケーション固有コード: {@link #getErrorCode()}</li>
 * <li>再試行候補かどうか: {@link #isRetryable()}</li>
 * </ul>
 */
public class RemoteApiException extends RestClientException {

    private final HttpStatusCode statusCode;
    private final ProblemDetail problemDetail;
    private final String rawErrorCode;
    private final RemoteApiErrorCode errorCode;

    protected RemoteApiException(String message, RestClientResponseException cause, ProblemDetail problemDetail) {
        super(message, cause);
        this.statusCode = cause.getStatusCode();
        this.problemDetail = problemDetail;
        this.rawErrorCode = extractRawErrorCode(problemDetail);
        this.errorCode = RemoteApiErrorCode.from(rawErrorCode);
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public ProblemDetail getProblemDetail() {
        return problemDetail;
    }

    public String getRawErrorCode() {
        return rawErrorCode;
    }

    public RemoteApiErrorCode getErrorCode() {
        return errorCode;
    }

    public String getTitle() {
        return problemDetail.getTitle();
    }

    public String getDetail() {
        return problemDetail.getDetail();
    }

    public String getPath() {
        return stringProperty(problemDetail, "path");
    }

    /**
     * このエラーが「再試行を検討しやすい種類か」を返します。
     * <p>
     * 判定はまずサーバ側の {@code ProblemDetail.code} を優先し、
     * それが取れない場合だけ 503 / 504 を補助的に見ています。
     * </p>
     * <p>
     * PoC では「厳密なリトライ戦略」までは持ち込まず、
     * まずは呼び出し側の分岐材料をそろえることを優先しています。
     * </p>
     *
     * @return 再試行候補なら {@code true}
     */
    public boolean isRetryable() {
        boolean retryableByCode = switch (errorCode) {
            case DB_LOCK_CONFLICT, DB_TIMEOUT, DB_UNAVAILABLE, OPTIMISTIC_LOCK_CONFLICT -> true;
            default -> false;
        };
        if (retryableByCode) {
            return true;
        }
        return statusCode.value() == 503 || statusCode.value() == 504;
    }

    /**
     * {@link ProblemDetail#getProperties()} から文字列プロパティを安全に取り出します。
     *
     * @param problemDetail 問題詳細
     * @param name 取得したいプロパティ名
     * @return 文字列値。未設定または空文字の場合は {@code null}
     */
    protected static String stringProperty(ProblemDetail problemDetail, String name) {
        Map<String, Object> properties = problemDetail.getProperties();
        if (properties == null) {
            return null;
        }
        Object value = properties.get(name);
        return value instanceof String text && StringUtils.hasText(text) ? text : null;
    }

    private static String extractRawErrorCode(ProblemDetail problemDetail) {
        return stringProperty(problemDetail, "code");
    }
}
