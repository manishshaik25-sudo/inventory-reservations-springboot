# Engineering note — retry-safe inventory reservations

**Problem pattern.** Warehouse applications receive repeated and concurrent order requests. A reservation must not reserve additional stock merely because a client retries after a network timeout, and two independent reservations must not both consume the final unit.

**Architecture.** `POST /reservations` requires `Idempotency-Key` and `{sku,quantity}`. A MySQL primary-key claim is inserted using `INSERT IGNORE` within a Spring-managed transaction. A previously committed key yields the stored status and reservation ID, or `409` if the request payload differs. The idempotency record, inventory change, and reservation row are committed together.

**Stock correctness.** The service reads SKU quantity and its version, then runs one conditional database update: `SET available_quantity = available_quantity - qty, version = version + 1 WHERE sku=? AND version=? AND available_quantity>=qty`. Only one request based on the same version can update a given stock row. A zero-row update returns a conflict without creating a reservation. This uses compare-and-swap semantics rather than trusting a prior read.

**What to test.** (1) Identical retry returns the first reservation and no additional deduction; (2) two independent concurrent requests against the final unit produce one success and one conflict; (3) update against stale version changes zero rows; (4) same key used with different quantity is rejected. Integration tests use real MySQL via Testcontainers.

**Optimistic vs pessimistic locking.** Version checks work well when conflicts are comparatively rare, avoiding locks across application computations. Under heavy contention, clients may see more conflicts and retry. Pessimistic row locking (`SELECT ... FOR UPDATE`) may simplify contended workflows but increases lock wait time and deadlock risk, especially for long transactions. A production design would measure conflict rate and database contention before choosing a policy.

**Limits and next steps.** Add TTL-based key retention, authenticated tenant-scoped keys, structured error bodies, request tracing/metrics, stock replenishment and release rules, time-limited reservations, cancellation, and load tests. A real warehouse integration also needs explicit handling for external systems and failure recovery; database transactions alone cannot coordinate external carrier/payment services.

**Professional context.** Motivated by general engineering patterns observed while working on Java logistics applications. This is an independent demonstration, not a statement about Manhattan Associates' or any previous employer's internal design.
