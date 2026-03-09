package com.example.poc.client.error;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Spring標準のHTTP例外変換を土台に、{@link ProblemDetail} を専用例外へ再マップします。
 * <p>
 * このクラスは、HTTPエラー時にゼロから独自判定を行うのではなく、
 * まず {@link DefaultResponseErrorHandler} に処理を委譲します。
 * これにより、Spring が提供する次の標準処理をそのまま利用できます。
 * </p>
 * <ul>
 * <li>4xx / 5xx をエラーとみなす判定</li>
 * <li>レスポンスボディやヘッダを保持した {@link RestClientResponseException} の生成</li>
 * <li>HTTP ステータスに応じた例外メッセージ組み立て</li>
 * </ul>
 * <p>
 * その上で、このクラスではレスポンスボディを {@link ProblemDetail} として読み直し、
 * クライアント側で扱いやすい {@link RemoteApiException} へ包み直します。
 * つまり役割分担は次の通りです。
 * </p>
 * <ul>
 * <li>HTTP レベルの例外化: Spring 標準</li>
 * <li>アプリケーション固有のエラー情報抽出: このクラス</li>
 * </ul>
 */
public class ProblemDetailResponseErrorHandler extends DefaultResponseErrorHandler {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Override
    protected void handleError(ClientHttpResponse response, HttpStatusCode statusCode, URI url, HttpMethod method)
            throws IOException {
        try {
            // まずは Spring 標準の例外化に任せる。
            super.handleError(response, statusCode, url, method);
        } catch (RestClientResponseException ex) {
            // 標準例外にレスポンス情報は揃っているので、
            // そこから ProblemDetail を復元して業務向け例外へ寄せる。
            throw translate(ex, url, method);
        }
    }

    private RemoteApiException translate(RestClientResponseException ex, URI url, HttpMethod method) {
        ProblemDetail problemDetail = readProblemDetail(ex);
        String message = buildMessage(method, url, ex, problemDetail);
        return new RemoteApiException(message, ex, problemDetail);
    }

    private ProblemDetail readProblemDetail(RestClientResponseException ex) {
        byte[] body = ex.getResponseBodyAsByteArray();
        if (body == null || body.length == 0) {
            // ボディなしエラーでも detail を持った例外にそろえるため、
            // ステータス文を使って簡易 ProblemDetail を組み立てる。
            return fallbackProblemDetail(ex);
        }

        try {
            ProblemDetail problemDetail = objectMapper.readValue(body, ProblemDetail.class);
            return problemDetail != null ? problemDetail : fallbackProblemDetail(ex);
        } catch (IOException parseFailure) {
            // サーバが ProblemDetail 以外を返した場合でも、
            // 呼び出し側が同じ例外型で扱えるようにフォールバックする。
            return fallbackProblemDetail(ex);
        }
    }

    private ProblemDetail fallbackProblemDetail(RestClientResponseException ex) {
        String detail = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
        if (!StringUtils.hasText(detail)) {
            detail = ex.getStatusText();
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), detail);
        if (StringUtils.hasText(ex.getStatusText())) {
            problemDetail.setTitle(ex.getStatusText());
        }
        return problemDetail;
    }

    private String buildMessage(HttpMethod method, URI url, RestClientResponseException ex, ProblemDetail problemDetail) {
        // ログや標準出力で状況を追いやすいよう、
        // 「HTTPメソッド / URL / ステータス / code / detail」を1行へ圧縮する。
        StringBuilder message = new StringBuilder();
        if (method != null) {
            message.append(method).append(' ');
        }
        if (url != null) {
            message.append(url).append(' ');
        }
        message.append("failed with ").append(ex.getStatusCode().value());

        if (StringUtils.hasText(problemDetail.getTitle())) {
            message.append(' ').append(problemDetail.getTitle());
        }

        String rawCode = RemoteApiException.stringProperty(problemDetail, "code");
        if (StringUtils.hasText(rawCode)) {
            message.append(" [").append(rawCode).append(']');
        }

        if (StringUtils.hasText(problemDetail.getDetail())) {
            message.append(": ").append(problemDetail.getDetail());
        }
        return message.toString();
    }
}
