# Spring Boot PoC (Gradle Multi Project)

このリポジトリは `common` / `server` / `client` / `server_old` の Gradle マルチプロジェクト構成です。

## 構成

- `common`: `OrderService` 契約、共有DTO、共有エンティティ（`OrderSearchView`）
- `server`: Spring Boot API サーバー（JPA / H2 / テストを含む）
- `client`: `RestClient + HttpServiceProxyFactory` で `server` の API を呼び出すクライアント
- `server_old`: Spring Framework 4.2 + Hibernate 5.1 の旧実装サンプル（CLI）

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
