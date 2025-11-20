# 📦 Kafka Orders Processing System (Docker + Java + Avro)

A complete Kafka-based real-time streaming system built using **Docker**, **Java**, and **Avro**.  
This project includes:

- Kafka + Zookeeper (via Docker)
- Avro-based Kafka Producer
- Kafka Consumer with:
  - Running average calculation
  - Retry logic (3 attempts)
  - Automatic DLQ routing
- DLQ Consumer for failed messages

---

## 📁 Project Structure

```
kafka-orders-assignment/
│
├── docker-compose.yml
├── pom.xml
│
└── src/main/java/com/assignment/kafka/
    ├── OrderProducer.java
    ├── OrderConsumer.java
    ├── DlqConsumer.java
    ├── AvroUtils.java
│
└── src/main/resources/avro/
    └── order.avsc
```

---

## 🏗 Architecture

```
               ┌────────────────────────┐
               │   OrderProducer (Java)  │
               └──────────────┬─────────┘
                              │
                   Kafka Topic: orders
                              │
               ┌──────────────▼──────────────┐
               │     OrderConsumer (Java)    │
               │ Running Avg + Retry Logic   │
               └──────────────┬─────────────┘
                              │ (Fails 3 times)
                              ▼
                   Kafka Topic: orders-dlq
                              │
                 ┌────────────▼────────────┐
                 │     DlqConsumer (Java)  │
                 └─────────────────────────┘
```

---

## 🛠 Tech Stack

| Component | Technology |
|----------|------------|
| Kafka Broker | Docker (Confluent Kafka) |
| Zookeeper | Docker |
| Serialization | Avro |
| Language | Java 11+ |
| Build Tool | Maven |
| DLQ | Kafka Topic |
| Retry Logic | Custom Consumer Code |

---

## 🐳 Docker Setup

### 1️⃣ Start Kafka + Zookeeper

Run from project root:

```sh
docker-compose up -d
```

Check containers:

```sh
docker ps
```

You should see Kafka + Zookeeper containers running.

### 2️⃣ Enter Kafka container

```sh
docker exec -it kafka-orders-assignment-kafka-1 bash
```

---

## 📌 Create Topics (inside container)

### Create main topic:

```sh
kafka-topics --bootstrap-server kafka:9092 --create --topic orders --partitions 3 --replication-factor 1
```

### Create DLQ topic:

```sh
kafka-topics --bootstrap-server kafka:9092 --create --topic orders-dlq --partitions 1 --replication-factor 1
```

---

## ▶ Run Java Services (3 Separate Terminals)

### 🟢 1️⃣ Start OrderConsumer

```sh
mvn exec:java -Dexec.mainClass=com.assignment.kafka.OrderConsumer
```

Expected:

```
OrderConsumer started. Listening to topic: orders
```

---

### 🔴 2️⃣ Start DLQ Consumer

```sh
mvn exec:java -Dexec.mainClass=com.assignment.kafka.DlqConsumer
```

Expected:

```
DlqConsumer started. Listening to orders-dlq
```

---

### 🟠 3️⃣ Start OrderProducer

```sh
mvn exec:java -Dexec.mainClass=com.assignment.kafka.OrderProducer
```

Example output:

```
Sent order 1001 - Item3 (52.10)
Sent order 1002 - Item5 (33.90)
```

---

## 🔁 DLQ Flow

### Consumer simulates failures:

```
Temporary failure processing key=1002, attempt 1/3
Temporary failure processing key=1002, attempt 2/3
Temporary failure processing key=1002, attempt 3/3
```

### After 3rd failure → Sent to DLQ:

```
Sent message with key=1002 to DLQ
```

### DLQ Consumer receives:

```
DLQ: key=1002 orderId=1002 product=Item3 price=33.90
```

---

## 🧬 Avro Schema (`order.avsc`)

```json
{
  "type": "record",
  "name": "Order",
  "namespace": "com.assignment",
  "fields": [
    { "name": "orderId", "type": "string" },
    { "name": "product", "type": "string" },
    { "name": "price", "type": "float" }
  ]
}
```

---

## 🔄 Retry Logic (Core Part of Assignment)

```java
for (int attempt = 1; attempt <= 3; attempt++) {
    try {
        process(order); // Success
        break;
    } catch (Exception e) { // Failure
        if (attempt == 3) {
            sendToDlq(order);  // Move to DLQ after 3rd failure
        }
    }
}
```

✔ 3 retries  
✔ On final failure → DLQ  

---

## 🏁 Conclusion

This project demonstrates:

- A working Kafka setup using **Docker**
- Avro serialization for structured order data
- Producer / Consumer with retry logic
- Dead Letter Queue system
- Real-time stream processing design

This solution fulfills all assignment requirements and provides a clean, reproducible environment using Docker.

---

> Need a **PDF report**, **GitHub description**, or **screenshots section** added?  
Just tell me — I can format everything for submission!
