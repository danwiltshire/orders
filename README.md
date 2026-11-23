# Order-processing demo

One Spring Boot image contains three independent entry points:
`com.example.orders.OrdersApplication`, `com.example.orders.CatalogApplication`,
and `com.example.orders.WorkerApplication`. The image's entrypoint supplies
the Java classpath; its argument selects the main class (default:
`OrdersApplication`). Compose sets it explicitly for each service. `SERVICE_ROLE`
is not used. All services listen on `SERVER_PORT` (default 8080). They run the
same Flyway migrations; Flyway's PostgreSQL lock serializes concurrent migrations.

## Run locally

Requires Docker and Docker Compose. From this directory:

```sh
docker compose up --build -d
docker compose ps
curl -fsS http://localhost:8081/api/catalog
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":2}'
# Use the returned Location header:
curl -fsS http://localhost:8080/api/orders/ORDER_ID
```

Open `http://localhost:3000` for the server-rendered catalog and order UI.
The UI builds from `ui/Dockerfile` and uses `CATALOG_API_URL` and
`ORDERS_API_URL` to reach the APIs server-side. The browser uses the same
origin for order requests. In Kubernetes, the ingress sends `/api/orders` and
`/api/catalog` directly to their services; locally the UI proxies order requests.

The order starts as `PENDING` and becomes `PROCESSED` asynchronously. Prices
are in integer cents and are captured at order creation; the UI displays them
as GBP for this demo. Invalid input returns
400; unknown products and orders return 404. The catalog is seeded on initial
migration. Orders, outbox records and RabbitMQ data persist in named volumes.
Run `docker compose down` to stop without deleting data; `docker compose down
-v` deletes the demo data.

The worker logs each order event at INFO, including its order ID and whether it
updated the order or skipped an already-processed delivery. An update log occurs
before the database transaction commits. To follow these logs, run
`docker compose logs -f worker`. Browsing the catalog or looking up an order
does not create a RabbitMQ event; creating an order does.

Compose credentials (`orders` / `local-orders` for PostgreSQL and `orders` /
`local-rabbit` for RabbitMQ) are **local demo credentials only**. Never reuse
them in production. For another deployment, supply secrets outside the image
through `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, `SPRING_RABBITMQ_HOST`,
`SPRING_RABBITMQ_USERNAME`, and `SPRING_RABBITMQ_PASSWORD`. Standard Spring
properties such as `SPRING_RABBITMQ_PORT` are also supported. No production
credentials are included.

## Reliability and health

Order creation and an outbox entry commit in one PostgreSQL transaction. The
orders role retries unpublished entries, using row locks to allow multiple
publishers. Messages are persistent, routed to a durable queue, and marked
published only after a positive RabbitMQ publisher confirm and no mandatory
return. The worker declares the exchange, queue and binding; until it is running,
publication remains in the outbox and is retried. A crash after confirmation
but before the outbox commit can produce duplicates. The worker's conditional
database update makes redelivery idempotent, and acknowledgement follows the
database transaction. Failed processing is requeued; persistent failures are
logged and require operator intervention. This demo does not implement a
dead-letter queue or automatic poison-message handling.

Flyway retries database connection failures at startup; persistent startup
errors fail the process. The listener reconnects and the outbox retries broker
failures during operation; persistent publication errors are logged.

Each role exposes `/healthz/live` and `/healthz/ready` on its own port (8080,
8081, and 8082 on the host). Liveness reports application state, not dependency
health. Readiness checks PostgreSQL for all roles and RabbitMQ for orders and
worker. The catalog does not require RabbitMQ. Health checks are observations,
not a guarantee that a later operation will succeed.

## Tests and architectures

With Java 21, Maven, and Docker available, run `mvn test`. The integration tests
use disposable Testcontainers PostgreSQL and RabbitMQ instances and are skipped
if Docker is unavailable. The image uses multi-architecture base images; for an
explicit ARM64 build run:

```sh
docker build --platform linux/arm64 -t orders-demo:arm64 .
```
