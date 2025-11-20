package com.assignment.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class OrderProducer {

    private static final String TOPIC = "orders";

    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:29092"); // from your Docker compose
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", ByteArraySerializer.class.getName());
        props.put("acks", "all");

        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(props)) {

            Random random = new Random();

            for (int i = 1; i <= 20; i++) {
                String orderId = String.valueOf(1000 + i);
                String product = "Item" + (1 + random.nextInt(5));
                float price = 10 + random.nextFloat() * 90; // 10.0 - 100.0

                GenericRecord order = AvroUtils.newOrderRecord(orderId, product, price);
                byte[] value = AvroUtils.serializeOrder(order);

                ProducerRecord<String, byte[]> record =
                        new ProducerRecord<>(TOPIC, orderId, value);

                try {
                    RecordMetadata metadata = producer.send(record).get();
                    System.out.printf("Sent order %s - %s (%.2f) to partition %d, offset %d%n",
                            orderId, product, price, metadata.partition(), metadata.offset());
                } catch (ExecutionException e) {
                    System.err.println("Failed to send message: " + e.getMessage());
                }

                Thread.sleep(500L);
            }
        }
    }
}
