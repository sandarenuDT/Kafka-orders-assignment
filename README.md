📦 Kafka Orders Assignment — Docker-Based Implementation
A simple, clean, and beginner-friendly README
This project demonstrates a Kafka-based real-time order processing system using Docker, Java, and Avro.
It includes:


Kafka + Zookeeper running in Docker


Java Producer sending random orders


Java Consumer with running average + retry logic


Dead Letter Queue (DLQ) handling for failed messages


DLQ Consumer to display failed events


The entire setup runs through Docker, so no manual Kafka installation is required.

📚 Table of Contents


What This Project Does


Architecture Diagram


Technology Stack


Project Structure


Setup Guide


Create Kafka Topics


Run the Java Programs


How DLQ Works


Avro Schema


Retry Logic


Conclusion



📝 What This Project Does
This assignment demonstrates a complete Kafka event pipeline:
✔ Producer
Sends order events to Kafka (orders topic) using Avro serialization.
✔ Consumer


Reads messages


Calculates a running average price


Randomly simulates failures


Retries failed events 3 times


Sends permanently failed messages to DLQ topic (orders-dlq)


✔ DLQ Consumer
Reads and prints messages from the Dead Letter Queue.
✔ Kafka + Zookeeper
Run using Docker containers for simplicity.

🏗 Architecture Diagram
Producer (Java)
     │
     ▼
 Kafka Topic: orders
     │
     ▼
Consumer (Java)
- Running Average
- Retry (3 times)
- Failure Simulation
     │
     ├── Success → continue
     └── Failure 3 times → DLQ
                     │
                     ▼
            Kafka Topic: orders-dlq
                     │
                     ▼
            DLQ Consumer (Java
