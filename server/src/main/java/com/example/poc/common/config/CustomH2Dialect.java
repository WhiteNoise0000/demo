package com.example.poc.common.config;

import java.sql.Types;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

// PoC では方言差分を差し込む入口としてカスタムDialectを明示しておく
public class CustomH2Dialect extends H2Dialect {

    @Override
    public JdbcType resolveSqlTypeDescriptor(
            String columnTypeName,
            int jdbcTypeCode,
            int precision,
            int scale,
            JdbcTypeRegistry jdbcTypeRegistry) {
        // Native Queryの集計結果(NUMERIC/DECIMAL)をLongとして受けたいケース向け
        if (jdbcTypeCode == Types.NUMERIC || jdbcTypeCode == Types.DECIMAL) {
            return jdbcTypeRegistry.getDescriptor(Types.BIGINT);
        }
        return super.resolveSqlTypeDescriptor(columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry);
    }
}
