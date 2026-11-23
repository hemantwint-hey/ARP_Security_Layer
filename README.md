# ARP Privacy Gateway (`arp_security_layer`)

The **ARP (Anonymized Routing & Privacy) Privacy Gateway** is the Trust Zone B microservice (`:8082`) for the **Enterprise Digital Banking System**. It enforces a strict **Zero-PII physical boundary** between the core Banking Monolith (`banking-app :8080`) and the Spring AI Cognitive Plane (`spring-ai-advisor-service :8083`).

All raw financial transactions are intercepted, tokenized, scrubbed of personally identifiable information (PII), and data-minimized before ever reaching the AI layer. AI insights and saving guard policies are then de-tokenized back to the customer inside this gateway.

---

## Core Architecture & Privacy Guarantees

* **Physical Database Isolation:** Exclusively owns `arp_privacy_db` (the encrypted identity vault). The downstream AI service (`ai_insights_db`) contains zero PII columns at the SQL schema level.
* **ARP Token Indirection (`ArpTokenResolverService`):** Replaces real `customerId` values with ephemeral anonymous tokens (e.g., `R789-9a4f8b21c0e3`), cached in Redis (`arp:map:{customerId}` / `arp:resolve:{arpToken}`), with support for token rotation and Right-to-Erasure revocation.
* **PII Scrubbing & Prompt-Injection Defense (`PiiScrubber`):** Strips emails, Indian mobile numbers, bank account numbers, and UPI IDs from free-text transaction remarks and advisor queries, and removes prompt-injection override phrases and delimiter attacks.
* **Anti-Linkage Data Minimization (`DataMinimizer`):** Coarsens exact timestamps into `timeBucket` (e.g., `WEEKDAY_NIGHT`) and `periodKey` (`yyyy-MM`), maps amounts into `amountBand` buckets, and rounds `minimizedAmount` to the nearest 500 in `BALANCED` mode (or omits numeric amounts completely in `STRICT` mode).
* **Cryptographic Protection (`crypto/`):**
  * `AesGcmEncryptor`: AES-256-GCM authenticated encryption for sensitive vault fields at rest.
  * `HmacClusterHasher`: Salted `HmacSHA256` deterministic hashing of normalized merchant descriptors so the AI can detect recurring subscriptions without seeing raw merchant names.
* **Active AI Saving Mode Sync (`SavingGuardSyncService`):** Combines user-configured spending caps with AI-recommended category locks and syncs them into Redis (`guard:policy:{customerId}`) for `< 2 ms` synchronous pre-transaction evaluation in the banking monolith.
* **Idempotent Event Processing:** Deduplicates Kafka events via `gateway_processed_events` inside `@Transactional` listeners, paired with `@RetryableTopic` (4 attempts, exponential backoff) and Dead-Letter Topics (`.DLT`).

---

## Tech Stack

* **Language & Framework:** Java 21, Spring Boot, Spring Security (JWT)
* **Messaging:** Apache Kafka 3.8 (KRaft), Jackson 3 (`tools.jackson.databind.ObjectMapper`)
* **Storage & Caching:** MySQL 8.0, Flyway Migrations, Redis 7
* **Observability:** Spring Boot Actuator, Prometheus, Grafana, OpenTelemetry / Zipkin

---

## Project Structure

```text
src/main/java/org/example/arp_ecurity_layer/
├── contracts/       # Kafka topic constants & shared event DTOs
├── crypto/          # AesGcmEncryptor & HmacClusterHasher
├── privacy/
│   ├── entity/      # ArpIdentityMapping, PrivacyAuditLog
│   ├── repository/  # JPA repositories for vault & audit logs
│   └── service/     # ArpTokenResolverService, PiiScrubber, DataMinimizer
├── guard/
│   ├── entity/      # CustomerSavingPreference, SavingGuardMetric
│   ├── dto/         # RedisGuardPolicyPayload
│   ├── repository/  # Preference & metric repositories
│   └── service/     # SavingGuardSyncService (Redis policy sync)
├── insights/
│   ├── entity/      # ResolvedCustomerInsight
│   └── repository/  # ResolvedCustomerInsightRepository
├── kafka/           # GatewayKafkaProducer & Kafka consumers (Raw, AI Result, Guard)
├── common/          # GatewayProcessedEvent (idempotency guard)
├── config/          # SecurityConfig, KafkaConfig, RedisConfig, JacksonConfig
└── web/             # REST controllers (Consent, Saving Mode, Insights, AI Advisor Proxy)

src/main/resources/
├── application.properties
├── application-dev.properties
└── db/migration/V1__init_arp_privacy_schema.sql
```

---

## Kafka Event Topology

| Topic | Direction | Partition Key | Purpose |
| :--- | :--- | :--- | :--- |
| `transaction.completed` | Inbound (from Monolith) | `accountId` | Raw banking transaction event (contains PII) |
| `arp.sanitized.transaction` | Outbound (to Spring AI) | `arpToken` | Zero-PII scrubbed & minimized spend event |
| `ai.insights.ready` | Inbound (from Spring AI) | `arpToken` | Subscription leaks, spend spikes, and savings tips |
| `ai.saving.guard.policy` | Inbound (from Spring AI) | `arpToken` | AI-computed spend caps & category block suggestions |
| `arp.identity.revoked` | Outbound (to Spring AI) | `arpToken` | Right-to-Erasure tombstone purging all `R789` records |
| `arp.guard.prevented` | Inbound (from Monolith) | `customerId` | Logs money saved when Saving Guard blocks/cancels outflow |
| `*.DLT` | Dead-Letter | Same as source | Poison-pill event inspection & manual replay |

---

## Database Schema (`arp_privacy_db`)

Managed automatically by Flyway (`V1__init_arp_privacy_schema.sql`):

1. `arp_identity_mappings` – Maps `customer_id` / `user_id` to `arp_token`, consent flag, `privacy_mode` (`BALANCED` | `STRICT`), and lifecycle status (`ACTIVE` | `ROTATED` | `REVOKED`).
2. `customer_saving_preferences` – Stores `guard_level` (`OFF` | `SOFT_GUARD` | `STRICT_LOCK`), monthly/single-tx caps, blocked categories, and `fail_closed_on_outage`.
3. `resolved_customer_insights` – De-tokenized AI insights ready for the customer dashboard.
4. `saving_guard_metrics` – Tracks prevented outflows and total money saved by AI Saving Mode.
5. `privacy_audit_logs` – Immutable audit trail of token minting, PII scrubbing, rotations, and revocations.
6. `gateway_processed_events` – Stores processed Kafka `event_id` values to prevent duplicate processing.

---

## Public REST APIs (`:8082`)

All endpoints require a valid **Bearer JWT** matching the Banking Monolith's signing secret. Customer identity is resolved strictly from JWT claims (never from request parameters).

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `PUT` | `/api/v1/privacy/consent` | Toggle AI consent and set privacy mode (`STRICT` / `BALANCED`) |
| `POST` | `/api/v1/privacy/identity/rotate` | Rotate active ephemeral `arpToken` |
| `POST` | `/api/v1/privacy/identity/revoke` | Erase identity mapping, evict Redis policy, and emit Kafka tombstone (`204 No Content`) |
| `PUT` | `/api/v1/saving-mode/preferences` | Configure guard level, caps, and blocked categories (immediately syncs Redis) |
| `GET` | `/api/v1/saving-mode/summary?period=YYYY-MM` | Fetch monthly money saved and blocked transaction metrics |
| `GET` | `/api/v1/ai-insights` | Paginated AI alerts and subscription leak recommendations |
| `PATCH` | `/api/v1/ai-insights/{id}/acknowledge` | Mark an insight as read/dismissed |
| `POST` | `/api/v1/ai-advisor/ask` | Scrub user question, resolve `arpToken`, and proxy Q&A to Spring AI |

---

## Configuration & Local Setup

### Prerequisites
* Java 21+ & Maven 3.9+
* Docker & Docker Compose (connected to `banking-network` with MySQL 8.0, Kafka 3.8, and Redis 7)

### Required Environment Variables

```properties
# Database (arp_privacy_db)
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/arp_privacy_db
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

# Kafka & Redis
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# Security & Cryptography
JWT_SECRET=same-jwt-secret-as-banking-app
ARP_CRYPTO_AES_SECRET_KEY=32-char-aes-gcm-secret-key-here!
ARP_CRYPTO_HMAC_SALT=your-high-entropy-hmac-salt-value
INTERNAL_AI_SERVICE_URL=http://localhost:8083
INTERNAL_AI_SERVICE_TOKEN=shared-internal-service-token
```

### Run Locally

```bash
# Build and run tests
mvn clean verify

# Start the gateway on port 8082 (dev profile)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
