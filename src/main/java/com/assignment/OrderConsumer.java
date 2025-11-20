package com.assignment.kafka;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;



import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;

public class OrderConsumer {

    private static final String INPUT_TOPIC = "orders";
    private static final String DLQ_TOPIC = "orders-dlq";
    private static final int MAX_RETRIES = 3;

    private static double totalPrice = 0.0;
    private static long count = 0;

    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        // Consumer configuration
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        // Producer for DLQ
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(consumerProps);
             KafkaProducer<String, byte[]> dlqProducer = new KafkaProducer<>(producerProps)) {

            consumer.subscribe(Collections.singletonList(INPUT_TOPIC));
            System.out.println("OrderConsumer started. Listening to topic: " + INPUT_TOPIC);

            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, byte[]> record : records) {
                    byte[] value = record.value();
                    String key = record.key();

                    boolean processed = processWithRetry(value, key, dlqProducer);
                    if (!processed) {
                        System.err.printf("Message with key=%s sent to DLQ after retries%n", key);
                    }
                }
            }
        }
    }

    private static boolean processWithRetry(byte[] value, String key, KafkaProducer<String, byte[]> dlqProducer) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                processMessage(value, key);
                return true;
            } catch (RuntimeException e) {
                System.err.printf("Temporary failure processing key=%s, attempt %d/%d: %s%n",
                        key, attempt, MAX_RETRIES, e.getMessage());
                try {
                    Thread.sleep(1000L * attempt); // exponential-ish backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Permanent failure → send to DLQ
        ProducerRecord<String, byte[]> dlqRecord = new ProducerRecord<>(DLQ_TOPIC, key, value);
        dlqProducer.send(dlqRecord, (metadata, exception) -> {
            if (exception != null) {
                System.err.printf("Failed to send message with key=%s to DLQ: %s%n",
                        key, exception.getMessage());
            } else {
                System.out.printf("Sent message with key=%s to DLQ partition=%d offset=%d%n",
                        key, metadata.partition(), metadata.offset());
            }
        });

        return false;
    }

    private static void processMessage(byte[] value, String key) {
        GenericRecord order = AvroUtils.deserializeOrder(value);
        String orderId = order.get("orderId").toString();
        String product = order.get("product").toString();
        float price = (Float) order.get("price");

        // Simulate temporary failure for assignment demo: e.g. randomly fail 20% of messages
        if (RANDOM.nextDouble() < 0.2) {
            throw new RuntimeException("Simulated temporary processing error");
        }

        // Update running average
        totalPrice += price;
        count++;
        double avg = totalPrice / count;

        System.out.printf("Processed Order: id=%s, product=%s, price=%.2f, RunningAvg=%.2f (count=%d)%n",
                orderId, product, price, avg, count);
    }
}
