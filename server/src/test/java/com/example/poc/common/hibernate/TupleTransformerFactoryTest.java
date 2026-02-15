package com.example.poc.common.hibernate;

import static org.assertj.core.api.Assertions.*;

import org.hibernate.query.TupleTransformer;
import org.junit.jupiter.api.Test;

import com.example.poc.dto.OrderStatusSummary;

class TupleTransformerFactoryTest {

    @Test
    void shouldCacheTransformerPerTargetType() {
        TupleTransformerFactory factory = new TupleTransformerFactory();

        TupleTransformer<OrderStatusSummary> first = factory.aliasToBean(OrderStatusSummary.class);
        TupleTransformer<OrderStatusSummary> second = factory.aliasToBean(OrderStatusSummary.class);

        assertThat(first).isSameAs(second);
    }
}

