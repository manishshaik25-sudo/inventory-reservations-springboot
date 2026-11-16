# Inventory Reservation Reliability Demo

A small **independent portfolio experiment** using Java 21, Spring Boot, MySQL, JPA, JUnit 5 and Testcontainers. Inspired by general warehouse/order-processing reliability problems—not affiliated with, evaluated by, or based on internal systems at Manhattan Associates or MillerKnoll.

## The engineering problem

A warehouse might have a single unit left while two requests try to reserve it. Clients may also retry a request after a timeout. The demo illustrates two complementary safeguards:

1. **Idempotency:** A MySQL primary key for `Idempotency-Key` serializes duplicate attempts. The server stores the response outcome inside the same transaction as the inventory decrement and reservation creation. An identical retry returns the original HTTP status and reservation data with `Idempotency-Replayed: true`. Reusing a key with different request details returns `409`.
2. **Optimistic concurrency:** The inventory row has a JPA `@Version` field. A single conditional SQL update decrements stock only if the observed version still matches *and* enough stock exists. A conflicting or insufficient-stock operation returns `409` rather than overselling.

## Running locally

Requirements: Java 21, Maven 3.6.3+, Docker Desktop or Docker Engine.

```bash
docker compose up -d
mvn spring-boot:run
```

On startup the demo seeds `LAPTOP-001` with quantity 1 **if it does not already exist**. MySQL is available at `localhost:3306`; adjust the port if your machine already runs MySQL.

## Try the API

```bash
curl -i -X POST http://localhost:8080/reservations \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: example-order-1001' \
  -d '{"sku":"LAPTOP-001","quantity":1}'
```

Expected first response: HTTP `201 Created`, status `RESERVED`, and a `reservationId`.

Run **exactly the same** command again. Expected: HTTP `201 Created`, same reservationId, and `Idempotency-Replayed: true` (no second decrement).

Use a **new key** with the same SKU and quantity. Expected: HTTP `409 Conflict` with `INSUFFICIENT_STOCK`.

## Run tests

```bash
mvn test
```

The JUnit + Testcontainers tests start **a real disposable MySQL 8.4 database** and check:

- Duplicate identical requests return the same result and create only one reservation.
- Two simultaneous different requests for the final unit cannot both succeed.
- A conditional inventory update with a stale version fails.
- Reusing an idempotency key for a different payload is rejected.

**Verification status:** Source files and test cases have been prepared. Tests were **not run** in the artifact-generation environment because Maven and Docker were unavailable. Run `mvn test` on your development machine before claiming the tests pass in outreach.

## Design choices and tradeoffs

| Choice | Why | Limitation |
|---|---|---|
| Database-backed idempotency key | Safe across app replicas using one database | The stored outcome is retained indefinitely in this demo; real services need expiry and retention policies |
| Single transaction for key, stock, reservation | Avoids a committed key without the corresponding reservation | Long-running calls must not occur inside the transaction |
| Version-checked conditional update | No row lock is held while deciding, avoids overwriting another reservation | Under high contention many requests may receive 409 and need client-side retry with a **new key** for a new attempt |
| Pessimistic locking alternative | `SELECT ... FOR UPDATE` serializes work for a single SKU | May create hotspots and longer lock waits for high-demand SKUs |
| Explicit result for retry | Original status/body is stable on repeated calls | Responses are reconstructed using recorded status, outcome, and reservation ID; this demo does not persist arbitrary response JSON |

## Deliberate scope limits

- This is a minimal demonstration, **not production-ready warehouse software**.
- No authentication, authorization, rate limiting, reservation expiry, release workflow, audit trail, or observability pipeline.
- `ddl-auto=update`, sample passwords, and `data.sql` are **local development conveniences only**.
- Concurrency testing with two threads illustrates a race scenario but is not a formal proof for all concurrent workloads.
- Invalid request bodies are rejected before an idempotency record is claimed.
- An exception that aborts the database transaction lets a client retry; this is different from a committed business conflict (409), which is replayed.

## Why I built this

I have worked on Java-backed logistics execution workflows involving REST APIs, SQL updates, and defect investigation. That experience motivated me to explore how inventory reservations can stay correct under retries and competing requests. No proprietary schemas, business rules, customer data, or company-specific architecture are used.

See [ENGINEERING_NOTE.md](ENGINEERING_NOTE.md) for a concise explanation of the system design and tradeoffs.
