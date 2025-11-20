package com.assignment.kafka;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericDatumReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AvroUtils {

    private static final Schema ORDER_SCHEMA;

    static {
        try (InputStream in = AvroUtils.class.getResourceAsStream("/avro/order.avsc")) {
            if (in == null) {
                throw new RuntimeException("order.avsc not found on classpath");
            }
            String schemaJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            ORDER_SCHEMA = new Schema.Parser().parse(schemaJson);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Avro schema", e);
        }
    }

    public static Schema getOrderSchema() {
        return ORDER_SCHEMA;
    }

    public static byte[] serializeOrder(GenericRecord record) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(ORDER_SCHEMA);
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        try {
            writer.write(record, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize Avro record", e);
        }
    }

    public static GenericRecord deserializeOrder(byte[] bytes) {
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(ORDER_SCHEMA);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(bytes), null);
        try {
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize Avro record", e);
        }
    }

    public static GenericRecord newOrderRecord(String orderId, String product, float price) {
        GenericRecord record = new GenericData.Record(ORDER_SCHEMA);
        record.put("orderId", orderId);
        record.put("product", product);
        record.put("price", price);
        return record;
    }
}
