# UPDATE PLAN

## 1. 目的

現行システムの古い Hibernate 依存を減らしつつ、Spring Boot + Spring Data JPA を軸に再整理する。
ただし、ネイティブ SQL・DTO 変換・一部の DML まで無理に JPA に寄せず、JPA と JDBC を責務分離したハイブリッド構成を採用する。

## 2. 採用方針

- Entity のライフサイクル管理は JPA / Spring Data JPA に寄せる
- DTO 取得、集計、ネイティブ SQL、バルク DML、動的 DDL は `NamedParameterJdbcTemplate` に寄せる
- Repository の無実装メソッドに `@JdbcTemplateQuery` を付け、既定で `sql/<RepositorySimpleName>.<methodName>.sql` を読む
- `Criteria` と移行過渡期の `saveOrUpdate` は custom fragment + `default` メソッドで吸収する
- Hibernate ネイティブ API への依存は可能な限り縮小する
- トランザクション境界は原則 service 層に置く
- ただし移行互換用の repository `default` メソッドには `@Transactional` を許容する

## 3. 置換方針

### 3.1 ORM / Query

1. `HQL` -> `JPQL`
   - JPQL に素直に落ちるものだけ移行する
   - Hibernate 固有構文や関数に強く依存するものは JDBC 側へ送る

2. 名前付き `HQL` -> `orm.xml` または `@Query`
   - 静的で外出ししたい query は `orm.xml`
   - 短く単純な query は `@Query` でも可

3. `hbm.xml` -> アノテーションマッピング
   - まずは annotation 化を進める
   - Hibernate 固有 mapping は個別に移行可否を判定する

4. `Hibernate Criteria` -> まず `JPA Criteria + custom fragment`
   - 既存の `Criteria` 資産は、まず `EntityManager` ベースの JPA Criteria へ素直に移植する
   - Spring Data JPA `Specification` は、新規に検索専用 repository を整理したい場合だけ選択肢にする
   - 既存 repository に寄せたいもの、少数の複雑検索は custom fragment + `default` + `EntityManager`
   - DTO projection、集計、ベンダ依存関数は JDBC 側へ送る

### 3.2 Native SQL

5. ネイティブクエリ -> `NamedParameterJdbcTemplate`
   - 基本方針は `1 row = 1 DTO`
   - 標準は `RowMapper`
   - 汎用マッピングは `DataClassRowMapper` / `BeanPropertyRowMapper`
   - 型変換や null 制御が重要な箇所だけ明示 `RowMapper`
   - Spring Data JPA Repository から呼びたいものは `@JdbcTemplateQuery` で束ねる

6. 名前付きネイティブクエリ XML -> `.sql` ファイルまたは Java text block へ移行
   - Repository 無実装メソッド向けは `sql/<DAO名>.<メソッド名>.sql` を既定にする
   - Java 側で条件組み立てが必要なものは text block を許容する
   - 旧来の XML query registry をそのまま再現しない

## 4. 採用しない方針

### 4.1 Hibernate `TupleTransformer` を主軸にしない

- `AliasToBeanResultTransformer` の後継として検討したが、Hibernate ネイティブ依存が残る
- 共通化は可能でも、長期的な移行方針としては JPA 標準化と逆方向になりやすい
- ネイティブ SQL の DTO 化は `NamedParameterJdbcTemplate + RowMapper` を本線とする

### 4.2 `default` メソッドを濫用しない

- repository interface の `default` メソッドは、移行互換 API と少数の `Criteria` 吸収に限定する
- `EntityManager` は custom fragment 実装からだけ供給し、service や呼び出し側へは露出しない
- ネイティブ SQL 実行の本体は `@JdbcTemplateQuery` / JDBC DAO 側へ寄せる

### 4.3 MyBatis は当面導入しない

- 現時点では `Spring Data JPA + NamedParameterJdbcTemplate` で責務分離できる
- `MyBatis Dynamic SQL` は Java DSL であり、2-way SQL や XML の `if` ベースとは思想が異なる
- 追加ライブラリを増やすより、text block + JDBC の方が現状に合う

## 5. 想定アーキテクチャ

### 5.1 JPA 側

- `Repository`
  - Entity の保存、取得、JPQL、`Specification`
- `Custom Fragment`
  - `default` メソッドによる `Criteria`
  - 移行互換の `saveOrUpdate`
  - 実装クラスは `EntityManager` 提供だけを担当

### 5.2 SQL 側

- `@JdbcTemplateQuery` 付き Repository メソッド
  - `sql/<DAO>.<method>.sql`
  - DTO 取得
  - 集計
- `*QueryDao` / `*SqlDao`
  - ネイティブ SQL
  - バルク DML
  - 動的 DDL

### 5.3 Service 側

- `@Transactional` を service 層に付与
- 1 ユースケース内で JPA と JDBC を束ねる

## 6. flush / 整合性ルール

- JPA 更新直後に JDBC で読む場合は `flush` を考慮する
- Do not auto-flush before every SQL execution
- 整合性が必要な入口だけ `flush` する
- `@JdbcTemplateQuery(flushAutomatically = true)` のような opt-in を基本にする
- JDBC で JPA 管理対象テーブルを直接更新した場合は、必要に応じて `clear` / `refresh` を考慮する

## 7. PoC の結論

- 本線は `Spring Data JPA + NamedParameterJdbcTemplate`
- ネイティブ SQL の DTO 化は `BeanPropertyRowMapper` を基本とする
- Repository の無実装メソッドは `@JdbcTemplateQuery` でかなり吸収できる
- 既存 `Hibernate Criteria` は `JPA Criteria + custom fragment` へ寄せるのが第一候補
- `Specification` は検索専用 repository を新規整理する場合に限定して使う
- `saveOrUpdate` は custom fragment + `default` メソッドで移行互換層を作れる
- `TupleTransformer` は PoC の比較対象にはなっても、本採用の主軸にはしない
- MyBatis は現時点では見送る

## 8. Next Actions

1. `@JdbcTemplateQuery` の対応範囲を決める
2. 単純 DML を `modifying` 相当でどこまで吸収するか決める
3. `saveOrUpdate` 呼び出し箇所を棚卸しし、最終的な廃止計画を作る
4. `flushAutomatically` が必要なユースケースを洗い出す
5. `hbm.xml` / HQL / native query / Criteria を、「JPA Criteria」「Specification」「JDBC」のどれへ送るか分類する
