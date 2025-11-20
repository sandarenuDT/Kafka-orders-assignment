package com.assignment.kafka;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class DlqConsumer {

    private static final String DLQ_TOPIC = "orders-dlq";

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(DLQ_TOPIC));
            System.out.println("DlqConsumer started. Listening to " + DLQ_TOPIC);

            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, byte[]> record : records) {
                    GenericRecord order = AvroUtils.deserializeOrder(record.value());
                    System.out.printf("DLQ: key=%s orderId=%s product=%s price=%s%n",
                            record.key(),
                            order.get("orderId"),
                            order.get("product"),
                            order.get("price"));
                }
            }
        }
    }
}
