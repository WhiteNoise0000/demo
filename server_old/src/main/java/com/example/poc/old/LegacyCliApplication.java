package com.example.poc.old;

import java.util.List;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.example.poc.old.config.LegacyHibernateConfig;
import com.example.poc.old.dto.LegacyOrderStatusSummary;
import com.example.poc.old.entity.LegacyPurchaseOrder;
import com.example.poc.old.service.LegacyOrderService;

/**
 * 旧実装を最短経路で確認するためのCLIエントリポイントです。
 * <p>
 * コンテナ起動 -> サンプル投入 -> 参照結果表示 の順で実行し、
 * Spring 4.2 + Hibernate 5.1 の動作確認を容易にしています。
 * </p>
 */
public class LegacyCliApplication {

    /**
     * @param args 未使用
     */
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(LegacyHibernateConfig.class)) {
            LegacyOrderService service = context.getBean(LegacyOrderService.class);
            service.bootstrapSampleData();

            List<LegacyPurchaseOrder> orders = service.findRecent(10);
            for (LegacyPurchaseOrder order : orders) {
                System.out.println(order.getId() + ", " + order.getCustomerName() + ", " + order.getStatus());
            }

            System.out.println("---- summary (AliasToBeanResultTransformer) ----");
            List<LegacyOrderStatusSummary> summaries = service.summarizeByStatusAliasToBean(null);
            for (LegacyOrderStatusSummary summary : summaries) {
                System.out.println(summary.getStatus()
                        + ", count=" + summary.getOrderCount()
                        + ", total=" + summary.getTotalSum());
            }
        }
    }
}
