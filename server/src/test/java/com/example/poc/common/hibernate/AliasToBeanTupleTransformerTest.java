package com.example.poc.common.hibernate;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.example.poc.dto.OrderStatusSummary;

class AliasToBeanTupleTransformerTest {

    @Test
    void shouldMapSnakeCaseAliasAndConvertNumberTypes() {
        AliasToBeanTupleTransformer<OrderStatusSummary> transformer =
                new AliasToBeanTupleTransformer<>(OrderStatusSummary.class);

        Object[] tuple = { "PAID", BigInteger.valueOf(3), new BigDecimal("960.00") };
        String[] aliases = { "status", "order_count", "total_sum" };

        OrderStatusSummary mapped = transformer.transformTuple(tuple, aliases);

        assertThat(mapped.getStatus()).isEqualTo("PAID");
        assertThat(mapped.getOrderCount()).isEqualTo(3L);
        assertThat(mapped.getTotalSum()).isEqualTo(960L);
    }

    @Test
    void shouldFailFastWhenTupleAndAliasLengthMismatch() {
        AliasToBeanTupleTransformer<OrderStatusSummary> transformer =
                new AliasToBeanTupleTransformer<>(OrderStatusSummary.class);

        Object[] tuple = { "PAID", BigInteger.valueOf(3) };
        String[] aliases = { "status" };

        assertThatThrownBy(() -> transformer.transformTuple(tuple, aliases))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("size mismatch");
    }

    @Test
    void shouldFailFastWhenTupleAndAliasAreEmpty() {
        AliasToBeanTupleTransformer<OrderStatusSummary> transformer =
                new AliasToBeanTupleTransformer<>(OrderStatusSummary.class);

        Object[] tuple = {};
        String[] aliases = {};

        assertThatThrownBy(() -> transformer.transformTuple(tuple, aliases))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void shouldFailFastWhenAliasIsUnknown() {
        AliasToBeanTupleTransformer<OrderStatusSummary> transformer =
                new AliasToBeanTupleTransformer<>(OrderStatusSummary.class);

        Object[] tuple = { "PAID", BigInteger.valueOf(3), new BigDecimal("960.00") };
        String[] aliases = { "status", "order_count", "unknown_total" };

        assertThatThrownBy(() -> transformer.transformTuple(tuple, aliases))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown SQL alias");
    }
}
