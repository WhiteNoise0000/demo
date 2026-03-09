# Spring Boot PoC (Gradle Multi Project)

このリポジトリは `common` / `server` / `client` / `server_old` の Gradle マルチプロジェクト構成です。

## 構成

- `common`: `OrderService` 契約、共有DTO、共有エンティティ（`OrderSearchView`）
- `server`: Spring Boot API サーバー（JPA / H2 / テストを含む）
- `client`: `RestClient + HttpServiceProxyFactory` で `server` の API を呼び出すクライアント
- `server_old`: Spring Framework 4.2 + Hibernate 5.1 の旧実装サンプル（CLI）

## client の例外処理 PoC

`client` プロジェクトでは、`@HttpExchange` クライアントのエラー処理を
できるだけ小さな構成で確認できるようにしています。

設計方針は次の通りです。

- Spring の Bean 定義は使わず、`OrderServiceClients.create(baseUrl)` で都度クライアント生成
- HTTP エラーは `ProblemDetailResponseErrorHandler` で一括処理
- まず Spring 標準の `DefaultResponseErrorHandler` に HTTP 例外化を委譲
- その後、レスポンスボディを `ProblemDetail` として読み直し、`RemoteApiException` へ統一変換
- 呼び出し側は例外クラスを細かく分けず、`status` / `errorCode` / `isRetryable()` を見て分岐

処理の流れは次の順です。

1. `OrderServiceClients` が `RestClient` に共通エラーハンドラを設定する
2. `HttpServiceProxyFactory` が `OrderService` の HTTP Interface 実装を生成する
3. API 呼び出しで 4xx / 5xx が返る
4. `DefaultResponseErrorHandler` が `RestClientResponseException` を生成する
5. `ProblemDetailResponseErrorHandler` がその例外から `ProblemDetail` を復元する
6. `RemoteApiException` が `status` / `code` / `detail` / `path` をまとめて保持する

呼び出し側の扱いは次のイメージです。

```java
try {
    client.namedJpqlByEntityManager("ali");
} catch (RemoteApiException ex) {
    switch (ex.getErrorCode()) {
        case OPTIMISTIC_LOCK_CONFLICT, DB_LOCK_CONFLICT -> {
            // 再読込やリトライを検討
        }
        case BAD_REQUEST -> {
            // 入力値の見直し
        }
        default -> {
            // 共通エラー処理
        }
    }
} catch (ResourceAccessException ex) {
    // 接続拒否、名前解決失敗、ソケットタイムアウトなど
}
```

この構成にしている理由は、PoC で見たいポイントが
「Spring の HTTP Interface をどう作るか」と
「サーバが返す `ProblemDetail` をどうクライアント例外へ寄せるか」
の2点だからです。Bean 化や複雑な例外階層はあえて持ち込まず、
流れが追いやすい最小構成に寄せています。

## 新実装の比較用クラス構成（server）

- API入口: `OrderPatternComparisonController`
- Spring Data パターン:
  - `SpringDataOrderSearchPattern`
  - `SpringDataOrderSearchViewRepository`
  - `OrderSearchSpecificationFactory`
- EntityManager パターン:
  - `EntityManagerOrderSearchPattern`
  - `EntityManagerOrderSearchDao`
  - `NativeQueryExecutor`
- MyBatis パターン:
  - `MyBatisOrderSearchPattern`
  - `OrderSearchMyBatisMapper`
  - `mappers/OrderSearchMyBatisMapper.xml`

`NativeQueryExecutor` は Hibernate `TupleTransformer` を内部で使い、
`SQL + named params + DTO class` だけで alias ベースの DTO マッピングを行う補助コンポーネントです。

MyBatis サンプルでは `<where>`, `<if>`, `<foreach>` を使い、
「条件が入ったときだけ WHERE / IN / 範囲条件を付与する」動的SQLを比較できます。

同じ `OrderService` 契約を使い、実装パターンだけを差し替えて比較できる構成です。

## 前提

- Java 17（`common` / `server` / `client` 用）
- Java 8（`server_old` 用）
- Windows の場合: `gradlew.bat` を利用

## 同一Gradleプロジェクトで異なるJDKの混在

可能です。  
このリポジトリでは Gradle Toolchains を使い、次のようにサブプロジェクトごとにJDKを分離しています。

- `server` / `client` / `common`: Java 17
- `server_old`: Java 8

例:

```powershell
.\gradlew.bat :server:compileJava
.\gradlew.bat :server_old:compileJava
```

## 実行例

### プロジェクト一覧

```powershell
.\gradlew.bat projects
```

### サーバーテスト

```powershell
.\gradlew.bat :server:test
```

### 旧実装（Spring 4.2 + Hibernate 5.1）のコンパイル

```powershell
.\gradlew.bat :server_old:compileJava
```

### 旧実装CLIの起動

```powershell
.\gradlew.bat :server_old:run
```

### クライアントのコンパイル

```powershell
.\gradlew.bat :client:compileJava
```

### サーバー起動

```powershell
.\gradlew.bat :server:bootRun
```

### クライアント実行

```powershell
.\gradlew.bat :client:run --args="http://localhost:8080"
```

`--args` を省略した場合は `http://localhost:8080` を利用します。  
環境変数 `POC_BASE_URL` でも上書きできます。
