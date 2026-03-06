# Hibernate 5.1 -> Spring Boot 3.5 / Spring Data JPA 移行メモ

更新日: 2026-03-07

## 背景

- 現行システムは Hibernate 5.1 単独採用
- API 面では Hibernate の `Session` / HQL / Native SQL / Hibernate Criteria / `hbm.xml` が同居
- 移行先は Spring Boot 3.5 + Spring Data JPA（Hibernate 6 系）
- 既存 SQL は約 600 本、Hibernate Criteria は約 8 本

## 結論

- `Spring Data JPA` だけで全資産を自然に受け止めるのは無理がある
- 特に `NativeQuery` 拡張や独自アノテーションで延命する案は、保守コストに対して利得が小さい
- 実務上の本命は `JPA/Hibernate + MyBatis` の併用
- 役割分担は以下を基本とする
  - JPA/Hibernate: entity 更新、ロック、version 管理、単純 CRUD、少数の entity 向け動的検索
  - MyBatis: 既存 SQL 資産、DTO 返却、一覧、集計、複雑 join、条件付き SQL

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

### MyBatis を併用する案

利点:

- SQL-first の設計にそのまま乗る
- `mapUnderscoreToCamelCase` で `order_count -> orderCount` を扱いやすい
- `<where>`, `<if>`, `<foreach>` で条件付き SQL を素直に書ける
- 600 本規模の SQL 資産を段階移行しやすい
- DTO 検索を Criteria へ無理に寄せなくてよい

課題:

- 永続化技術が 1 つ増える
- repository レイヤを 1 つの技術で完全統一はしにくい
- チームに MyBatis の流儀を導入する必要がある

## 資産分類ごとの推奨移行先

| 現行資産 | 典型例 | 推奨移行先 | 方針 |
|---|---|---|---|
| Hibernate HQL | entity を素直に取得する検索 | Spring Data JPA `@Query` / repository method | 残せるものは JPA へ |
| Hibernate HQL | DTO `select new`、複雑 join、DB 関数多用 | MyBatis | SQL-first に寄せる |
| Hibernate Criteria | entity を返す動的検索 | JPA Criteria / `Specification` | 少数だけ移行 |
| Hibernate Criteria | DTO 返却、複雑 join、集計、画面検索 | MyBatis | Criteria 継続しない |
| Hibernate Native SQL | DTO 返却、帳票、一覧、集計 | MyBatis | 主戦場 |
| `AliasToBeanResultTransformer` | alias -> DTO setter 自動移送 | MyBatis `resultType` | 置き換え対象 |
| `hbm.xml` の `<query>` | Named HQL | `@Query` または `orm.xml` | 段階的廃止 |
| `hbm.xml` の `<sql-query>` | Named native SQL | MyBatis | 廃止対象 |
| `hbm.xml` の entity mapping | XML マッピング | annotation または `orm.xml` | 新規採用しない |
| `saveOrUpdate`, lock, version 更新 | 集約更新 | JPA repository / custom impl | JPA へ |

## 判定ルール

- 返り値が `Entity` で、更新や整合性の文脈が強いなら JPA
- 返り値が `DTO` で、SQL を読んだ方が早いなら MyBatis
- 条件付き `WHERE`、`IN`、集計、画面一覧、複雑 join は MyBatis
- `hbm.xml` は移行中の橋までは許容しても、本命の置き場にはしない
- `Hibernate Criteria` 8 本は機械的に全部 JPA Criteria 化しない

## API 面の整理

- 現行は Hibernate 単独で HQL / SQL / Criteria が同一 API 面に同居している
- 移行後も、呼び出し側に見せる API 面は 1 つに保てる
- ただし、同一の repository interface を JPA と MyBatis が同時に実装する設計は取らない
- 代わりに service / query facade / custom repository fragment を統一面にして、内部で JPA と MyBatis を使い分ける

## hbm.xml の扱い

- `hbm.xml` は今回の移行では「橋」にはなっても「着地点」には向かない
- Spring Data JPA の正規ルートは `annotation` / `orm.xml` / `@Query`
- `hbm.xml` 内の native SQL や named query を新基盤で延命するより、MyBatis へ逃がした方が筋がよい

## この PoC で追加した比較材料

### Hibernate NativeQuery の簡略実行

- `server/src/main/java/com/example/poc/common/hibernate/NativeQueryExecutor.java`
- Hibernate `TupleTransformer` を包み、`SQL + named params + DTO class` だけで DTO マッピングするサンプル
- 「Spring Data JPA の NativeQuery を頑張るとどうなるか」の参考実装

### MyBatis サンプル

- `server/src/main/java/com/example/poc/dao/mybatis/OrderSearchMyBatisMapper.java`
- `server/src/main/resources/mappers/OrderSearchMyBatisMapper.xml`
- `server/src/main/java/com/example/poc/service/pattern/MyBatisOrderSearchPattern.java`

確認できるポイント:

- `<where>` により条件が 1 つもない場合は `WHERE` 自体を出さない
- `<if>` により、パラメータ指定時のみ条件を付与する
- `<foreach>` により `IN (...)` を動的生成できる
- `mapUnderscoreToCamelCase=true` により DTO / JavaBean マッピングを簡略化できる

## 推奨移行順

1. `hbm.xml` の `<sql-query>` と `AliasToBeanResultTransformer` 系を MyBatis へ移す
2. Hibernate Criteria 8 本を棚卸しし、entity 向けのみ JPA Criteria / `Specification` に残す
3. DTO 検索、集計、一覧、複雑 join は MyBatis へ寄せる
4. 単純 HQL は Spring Data JPA `@Query` または repository method へ移す
5. 更新系、ロック、version 管理は JPA 側に集約する

## 最終方針

- `JPA NativeQuery` の拡張を主戦略にはしない
- `JPA/Hibernate + MyBatis` の併用を正式方針とする
- API 面は service / facade で統一し、永続化技術面は無理に 1 つへ揃えない
