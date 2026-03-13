# UPDATE PLAN

## 1. 目的

現行システムの古い Hibernate 依存を減らしつつ、Spring Boot + Spring Data JPA を軸に再整理する。
ただし、ネイティブ SQL・DTO 変換・DML・動的 DDL まで無理に JPA に寄せず、JPA と JDBC を責務分離したハイブリッド構成を採用する。

## 2. 採用方針

- Entity のライフサイクル管理は JPA / Spring Data JPA に寄せる
- DTO 取得、集計、ネイティブ SQL、バルク DML、動的 DDL は `NamedParameterJdbcTemplate` に寄せる
- Hibernate ネイティブ API への依存は可能な限り縮小する
- トランザクション境界は service 層に集約する

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

4. `Criteria` -> Spring Data JPA `Specification`
   - Entity 検索条件の動的組み立てに限定して利用する
   - 複雑な DTO projection、集計、ベンダ依存関数は JDBC 側へ送る

### 3.2 Native SQL

5. ネイティブクエリ -> `NamedParameterJdbcTemplate`
   - 基本方針は `1 row = 1 DTO`
   - 標準は `RowMapper`
   - 汎用マッピングは `BeanPropertyRowMapper`
   - 型変換や null 制御が重要な箇所だけ明示 `RowMapper`

6. 名前付きネイティブクエリ XML -> Java text block ベースへ移行
   - まずは Java の text block で SQL を保持する
   - 長大 SQL の外出しが必要になった場合のみ `.sql` ファイル化を検討する
   - 旧来の XML query registry をそのまま再現しない

## 4. 採用しない方針

### 4.1 Hibernate `TupleTransformer` を主軸にしない

- `AliasToBeanResultTransformer` の後継として検討したが、Hibernate ネイティブ依存が残る
- 共通化は可能でも、長期的な移行方針としては JPA 標準化と逆方向になりやすい
- ネイティブ SQL の DTO 化は `NamedParameterJdbcTemplate + RowMapper` を本線とする

### 4.2 `default` メソッドで `EntityManager` を直接扱わない

- repository interface の `default` メソッドは薄い補助ロジックに限定する
- `EntityManager` や Hibernate `Session` を公開 API に漏らさない
- ネイティブ SQL 実行ロジックは JPA repository ではなく SQL 専用層へ寄せる

### 4.3 MyBatis は当面導入しない

- 現時点では `Spring Data JPA + NamedParameterJdbcTemplate` で責務分離できる
- `MyBatis Dynamic SQL` は Java DSL であり、2-way SQL や XML の `if` ベースとは思想が異なる
- 追加ライブラリを増やすより、text block + JDBC の方が現状に合う

## 5. 想定アーキテクチャ

### 5.1 JPA 側

- `Repository`
  - Entity の保存、取得、JPQL、`Specification`

### 5.2 SQL 側

- `*QueryDao` / `*SqlDao`
  - ネイティブ SQL
  - DTO 取得
  - 集計
  - バルク DML
  - 動的 DDL

### 5.3 Service 側

- `@Transactional` を service 層に付与
- 1 ユースケース内で JPA と JDBC を束ねる

## 6. flush / 整合性ルール

- JPA 更新直後に JDBC で読む場合は `flush` を考慮する
- Do not auto-flush before every SQL execution
- 整合性が必要な入口だけ `flush` する
- JDBC で JPA 管理対象テーブルを直接更新した場合は、必要に応じて `clear` / `refresh` を考慮する

## 7. PoC の結論

- 本線は `Spring Data JPA + NamedParameterJdbcTemplate`
- ネイティブ SQL の DTO 化は `BeanPropertyRowMapper` を基本とする
- `TupleTransformer` は PoC の比較対象にはなっても、本採用の主軸にはしない
- MyBatis は現時点では見送る

## 8. Next Actions

1. JPA と JDBC の責務境界を package / class レベルで定義する
2. 代表的なネイティブ SQL を `NamedParameterJdbcTemplate + BeanPropertyRowMapper` で PoC 実装する
3. 複雑な動的 SQL を 1 本選び、text block + 条件組み立てで保守可能か確認する
4. `flush` が必要なユースケースを洗い出し、service 側での運用ルールを決める
5. `hbm.xml` / HQL / native query を移行分類表に落とし込む
