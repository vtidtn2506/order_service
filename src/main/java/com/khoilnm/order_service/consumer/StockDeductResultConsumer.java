package com.khoilnm.order_service.consumer;

import com.khoilnm.order_service.constant.KafkaTopic;
import com.khoilnm.order_service.dto.event.StockDeductResultEvent;
import com.khoilnm.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDeductResultConsumer {
    private final OrderService orderService;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            exclude = { IllegalArgumentException.class }
    )
    @KafkaListener(topics = KafkaTopic.STOCK_DEDUCT_RESULT)
    public void onStockDeductResult(ConsumerRecord<String, StockDeductResultEvent> record) {
        StockDeductResultEvent result = record.value();
        log.info("Received {} (partition={}, offset={}): orderId={}, status={}, reason={}",
                record.topic(), record.partition(), record.offset(),
                result.orderId(), result.status(), result.reason());
        orderService.applyStockDeductResult(result);
    }
}
