# Hibernate 5.1 -> Spring Boot 3.5 / Spring Data JPA 移行メモ

更新日: 2026-03-24

## 背景

- 現行システムは Hibernate 5.1 単独採用
- API 面では Hibernate の `Session` / HQL / Native SQL / Hibernate Criteria / `hbm.xml` が同居
- 移行先は Spring Boot 3.5 + Spring Data JPA（Hibernate 6 系）
- 既存 SQL は約 600 本、Hibernate Criteria は約 8 本

## 結論

- `Spring Data JPA` だけで全資産を自然に受け止めるのは無理がある
- ただし、Repository 拡張と `NamedParameterJdbcTemplate` を併用すれば、DTO SQL 資産はかなり素直に受け止められる
- 実務上の本命は `JPA/Hibernate + Spring JDBC` の併用
- 役割分担は以下を基本とする
  - JPA/Hibernate: entity 更新、ロック、version 管理、単純 CRUD、少数の entity 向け動的検索
  - Spring JDBC: 既存 SQL 資産、DTO 返却、一覧、集計、複雑 join、条件付き SQL

## 判断理由

### Spring Data JPA の NativeQuery を頑張る案

利点:

- 永続化技術の見た目を 1 つに寄せやすい
- 単純な JPQL / entity 操作との親和性は高い

課題:

- `@NativeQuery` を alias-to-bean 的に拡張するには内部実装へ深く踏み込む必要がある
- `sqlResultSetMapping` なしで DTO 自動移送を安定提供するのは重い
- `Pageable` / `Sort` / `countQuery` / パラメータ解決まで面倒を見ると小さなサブフレームワーク化しやすい
- Hibernate 依存実装になりやすく、JPA 標準としては閉じない

### Spring JDBC を併用する案

利点:

- SQL-first の設計にそのまま乗る
- `DataClassRowMapper` / `BeanPropertyRowMapper` で `order_count -> orderCount` を扱いやすい
- `sql/<DAO>.<method>.sql` 規約で SQL ファイルをそのまま置きやすい
- 600 本規模の SQL 資産を段階移行しやすい
- DTO 検索を Criteria へ無理に寄せなくてよい
- Spring Data JPA Repository の無実装メソッドへ独自アノテーションを付けて実行基盤を共通化できる

課題:

- 永続化技術が 1 つ増える
- Repository 拡張を深くしすぎると小さな自前フレームワーク化しやすい
- `Pageable` / `Sort` / 更新系 DML / generated key 回収まで抱えると実装量が増える

## 資産分類ごとの推奨移行先

| 現行資産 | 典型例 | 推奨移行先 | 方針 |
|---|---|---|---|
| Hibernate HQL | entity を素直に取得する検索 | Spring Data JPA `@Query` / repository method | 残せるものは JPA へ |
| Hibernate HQL | DTO `select new`、複雑 join、DB 関数多用 | Spring JDBC | SQL-first に寄せる |
| Hibernate Criteria | entity を返す動的検索 | JPA Criteria / custom fragment | まずは素直移植 |
| Hibernate Criteria | DTO 返却、複雑 join、集計、画面検索 | Spring JDBC | Criteria 継続しない |
| Hibernate Native SQL | DTO 返却、帳票、一覧、集計 | Spring JDBC | 主戦場 |
| `AliasToBeanResultTransformer` | alias -> DTO setter 自動移送 | `DataClassRowMapper` / `BeanPropertyRowMapper` | 置き換え対象 |
| `hbm.xml` の `<query>` | Named HQL | `@Query` または `orm.xml` | 段階的廃止 |
| `hbm.xml` の `<sql-query>` | Named native SQL | Spring JDBC | 廃止対象 |
| `hbm.xml` の entity mapping | XML マッピング | annotation または `orm.xml` | 新規採用しない |
| `saveOrUpdate`, lock, version 更新 | 集約更新 | JPA repository / custom fragment | JPA へ |

## 判定ルール

- 返り値が `Entity` で、更新や整合性の文脈が強いなら JPA
- 返り値が `DTO` で、SQL を読んだ方が早いなら Spring JDBC
- 条件付き `WHERE`、`IN`、集計、画面一覧、複雑 join は Spring JDBC
- `hbm.xml` は移行中の橋までは許容しても、本命の置き場にはしない
- `Hibernate Criteria` 8 本は、まず JPA Criteria + custom fragment で素直移植を検討する
- `Specification` は検索専用 repository を新規整理したい場合だけ選択肢にする

## API 面の整理

- 現行は Hibernate 単独で HQL / SQL / Criteria が同一 API 面に同居している
- 移行後も、呼び出し側に見せる API 面は 1 つに保てる
- ただし、同一の repository interface を JPA と Spring JDBC が同時に実装する設計は取りにくい
- 代わりに service / query facade / custom repository fragment を統一面にして、内部で JPA と Spring JDBC を使い分ける

## hbm.xml の扱い

- `hbm.xml` は今回の移行では「橋」にはなっても「着地点」には向かない
- Spring Data JPA の正規ルートは `annotation` / `orm.xml` / `@Query`
- `hbm.xml` 内の native SQL や named query を新基盤で延命するより、Spring JDBC へ逃がした方が筋がよい

## この PoC で追加した比較材料

### Hibernate NativeQuery の簡略実行

- `server/src/main/java/com/example/poc/common/hibernate/NativeQueryExecutor.java`
- Hibernate `TupleTransformer` を包み、`SQL + named params + DTO class` だけで DTO マッピングするサンプル
- 「Spring Data JPA の NativeQuery を頑張るとどうなるか」の参考実装

### Repository 拡張 + Spring JDBC サンプル

- `server/src/main/java/com/example/poc/dao/jdbcquery/JdbcTemplateQuery.java`
- `server/src/main/java/com/example/poc/dao/jdbcquery/JdbcTemplateRepositoryQuery.java`
- `server/src/main/resources/sql/PurchaseOrderRepository.summarizeByStatusWithJdbc.sql`
- `server/src/main/java/com/example/poc/dao/PurchaseOrderRepositoryCustom.java`

確認できるポイント:

- `@JdbcTemplateQuery` により、無実装 Repository メソッドを `NamedParameterJdbcTemplate` 実行へ振り分けられる
- `value` 未指定時に `sql/<Repository>.<method>.sql` 規約で SQL を読める
- custom fragment の `default` メソッドから `EntityManager` を呼び、Criteria API を interface 側へ寄せられる
- 移行過渡期の `saveOrUpdate` を default メソッドとして残し、実装クラスは `EntityManager` 提供だけにできる

## 推奨移行順

1. `hbm.xml` の `<sql-query>` と `AliasToBeanResultTransformer` 系を Spring JDBC へ移す
2. Hibernate Criteria 8 本を棚卸しし、まず entity 向けは JPA Criteria + custom fragment へ寄せる
3. DTO 検索、集計、一覧、複雑 join は Spring JDBC へ寄せる
4. 単純 HQL は Spring Data JPA `@Query` または repository method へ移す
5. 更新系、ロック、version 管理は JPA 側に集約する

## 最終方針

- `JPA NativeQuery` の拡張を主戦略にはしない
- `JPA/Hibernate + Spring JDBC` の併用を正式方針とする
- API 面は service / facade で統一し、永続化技術面は無理に 1 つへ揃えない
