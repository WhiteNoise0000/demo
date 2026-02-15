package com.example.poc.common.handler;

import java.time.OffsetDateTime;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 例外をHTTPレスポンスへ統一変換するグローバルハンドラです。
 * <p>
 * 永続化層・入力値検証・予期しない例外を {@link ProblemDetail} 形式に正規化し、
 * クライアント側で判定しやすいアプリケーションコード（{@code code}）を付与します。
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 楽観排他競合を409 Conflictへ変換します。
     *
     * @param ex 例外本体
     * @param request リクエスト情報
     * @return 問題詳細レスポンス
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex, WebRequest request) {
        return problem(HttpStatus.CONFLICT, "Optimistic lock conflict",
                "The target data was updated by another transaction. Retry the operation.",
                "OPTIMISTIC_LOCK_CONFLICT", request, ex);
    }

    /**
     * DBクエリタイムアウト/トランザクションタイムアウトを504 Gateway Timeoutへ変換します。
     *
     * @param ex 例外本体
     * @param request リクエスト情報
     * @return 問題詳細レスポンス
     */
    @ExceptionHandler({
            QueryTimeoutException.class,
            TransactionTimedOutException.class
    })
    ProblemDetail handleTimeout(RuntimeException ex, WebRequest request) {
        return problem(HttpStatus.GATEWAY_TIMEOUT, "Database timeout",
                "Database operation timed out. Retry later.",
                "DB_TIMEOUT", request, ex);
    }

    /**
     * ロック獲得失敗を409 Conflictへ変換します。
     *
     * @param ex 例外本体
     * @param request リクエスト情報
     * @return 問題詳細レスポンス
     */
    @ExceptionHandler({
            CannotAcquireLockException.class
    })
    ProblemDetail handleLockConflict(DataAccessException ex, WebRequest request) {
        return problem(HttpStatus.CONFLICT, "Lock conflict",
                "Concurrent update conflict occurred. Retry the operation.",
                "DB_LOCK_CONFLICT", request, ex);
    }

    /**
     * DB接続不可/DBリソース障害を503 Service Unavailableへ変換します。
     *
     * @param ex 例外本体
     * @param request リクエスト情報
     * @return 問題詳細レスポンス
     */
    @ExceptionHandler({
            CannotGetJdbcConnectionException.class,
            DataAccessResourceFailureException.class
    })
    ProblemDetail handleDbUnavailable(DataAccessException ex, WebRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Database unavailable",
                "Database is temporarily unavailable.",
                "DB_UNAVAILABLE", request, ex);
    }

    /**
     * 不正なリクエストパラメータを400 Bad Requestへ変換します。
     *
     * @param ex 例外本体
     * @param request リクエスト情報
     * @return 問題詳細レスポンス
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class
    })
    ProblemDetail handleBadRequest(Exception ex, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request",
                ex.getMessage(),
                "BAD_REQUEST", request, ex);
    }

    /**
     * 上記で個別に扱わないデータアクセス例外を500 Internal Server Errorへ変換します。
     *
     * @param ex 例外本体
     * @param request リクエスト情報
     * @return 問題詳細レスポンス
     */
    @ExceptionHandler(DataAccessException.class)
    ProblemDetail handleDataAccess(DataAccessException ex, WebRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Database error",
                "Database processing failed.",
                "DB_ERROR", request, ex);
    }

    /**
     * 想定外の例外を500 Internal Server Errorへ変換します。
     *
     * @param ex 例外本体
     * @param request リクエスト情報
     * @return 問題詳細レスポンス
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error",
                "Unexpected server error occurred.",
                "INTERNAL_ERROR", request, ex);
    }

    /**
     * 問題詳細レスポンスを組み立てます。
     *
     * @param status HTTPステータス
     * @param title 問題タイトル
     * @param detail 問題の詳細
     * @param code アプリケーション固有エラーコード
     * @param request リクエスト情報
     * @param ex 例外本体
     * @return 問題詳細レスポンス
     */
    private ProblemDetail problem(HttpStatus status, String title, String detail, String code, WebRequest request,
            Exception ex) {
        ex.printStackTrace();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("code", code);
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        pd.setProperty("path", pathOf(request));
        pd.setProperty("exception", ex.getClass().getSimpleName());
        return pd;
    }

    /**
     * リクエストパスを取得します。
     *
     * @param request リクエスト情報
     * @return URIパス。取得できない場合は {@code N/A}
     */
    private String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return "N/A";
    }
}
