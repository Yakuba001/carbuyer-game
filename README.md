# Carbuyer — Multi-User Car Market Simulator

A concurrent backend service simulating an automotive marketplace where authenticated users trade vehicles alongside autonomous market-maker bots.

Built with **Java 21**, **Spring Boot 3.3**, **PostgreSQL 16**, and **Virtual Threads (Project Loom)**, designed around strict module isolation, deadlock-free transactional execution, and database-enforced invariants.

---

## Tech Stack

* **Core:** Java 21 (Virtual Threads), Spring Boot 3.3.3 (Web, Data JPA, Security, Validation, Actuator), Gradle (Kotlin DSL)
* **Database & Migrations:** PostgreSQL 16, Flyway, Spring JDBC (`NamedParameterJdbcTemplate`)
* **Security & API Docs:** JJWT (`0.12.6`), Springdoc OpenAPI / Swagger UI (`2.6.0`)
* **Testing:** JUnit 5, Testcontainers (`postgres:16-alpine`), MockMvc, `TestRestTemplate`, Awaitility
* **Infrastructure:** Docker (multi-stage Alpine build), Docker Compose, GitHub Actions CI

---

## Architecture & Engineering Decisions

### 1. Modular Monolith with Explicit Facade Boundaries
The codebase is partitioned into isolated domain modules (`user`, `car`, `garage`, `market`, `bot`, `security`). Cross-module communication is restricted exclusively to domain **Facade interfaces** (`UserFacade`, `CarFacade`, `GarageFacade`, `MarketFacade`), preventing direct repository or entity coupling across boundaries.

```text
┌─────────────────────────────────────────────────────────────────┐
│                          REST API Layer                         │
│        AuthController   │   MarketController   │   BotController│
└────────────┬────────────┴──────────┬───────────┴───────┬────────┘
             │                       │                   │
             ▼                       ▼                   ▼
      ┌─────────────┐         ┌─────────────┐     ┌─────────────┐
      │   Security  │         │   Market    │────▶│     Bot     │
      │    (JWT)    │         │   Service   │     │   Engine    │
      └──────┬──────┘         └──────┬──────┘     └──────┬──────┘
             │                       │                   │
             │        ┌──────────────┼───────────────────┘
             │        │   (Strict Facade Boundaries)
             ▼        ▼
   ┌───────────────────────┐   ┌───────────────┐   ┌──────────────┐
   │      UserFacade       │   │   CarFacade   │   │ GarageFacade │
   └───────────────────────┘   └───────────────┘   └──────────────┘
```

### 2. Concurrency & Deadlock Prevention
* **Deterministic Lock Ordering:** During atomic car purchases (`MarketService.buyCar`), balances and garage slots for buyer and seller are locked in deterministic ascending order of user IDs (`Math.min(buyerId, sellerId)` -> `Math.max(buyerId, sellerId)`) via `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`), eliminating circular-wait deadlocks under concurrent trades.
* **Contention-Free Listing Selection:** Bots pick random active listings using PostgreSQL `FOR UPDATE SKIP LOCKED`, preventing worker threads from blocking on contended rows.
* **Virtual Thread Simulation Engine:** `BotService` runs autonomous workers on `Executors.newVirtualThreadPerTaskExecutor()` with `in-flight` state tracking (`ConcurrentUniqueQueue<T>`), worker-to-bot capping (`50%`) to prevent thread starvation, dynamic buyer/seller ratio rebalancing (`30%` threshold -> `50%` target), and graceful shutdown via `CountDownLatch`.

### 3. Database Integrity & Batch Performance
* **Schema-Level Invariants (PostgreSQL + Flyway):**
  * `CHECK` constraints enforcing valid mileage (`>= 0`), condition score (`0..100`), non-negative prices, status-dependent `closed_at`, and anti-self-purchase (`seller_id <> buyer_id`).
  * `ON DELETE RESTRICT / CASCADE / SET NULL` referential rules and JPA optimistic locking (`@Version`).
* **Partial & Unique Indexes:**
  * `idx_market_listings_one_active_car` — guarantees at most one `ACTIVE` listing per vehicle while retaining historical `SOLD` / `CANCELLED` records.
  * `idx_market_listings_feed` — partial index on `(created_at DESC) WHERE status = 'ACTIVE'` for paginated marketplace queries.
  * `idx_garage_slots_car_unique` — prevents assigning the same vehicle to multiple garage slots.
* **Batch Execution:** Bulk bot provisioning, vehicle generation, and slot allocation use `NamedParameterJdbcTemplate.batchUpdate` with `GeneratedKeyHolder` and Hibernate sequence batching (`allocationSize = 50`).

### 4. Stateless Security
* JWT authentication (`access` + `refresh` tokens) implemented via custom `OncePerRequestFilter` (`JwtAuthenticationFilter`) and `BCryptPasswordEncoder`.
* Role-based authorization restricting simulation control endpoints (`/api/v1/bot/**`) to `ROLE_ADMIN`.
* Centralized exception-to-HTTP mapping via `@RestControllerAdvice` returning structured `ErrorResponse` payloads.

---

## API Endpoints

Interactive OpenAPI documentation is available at `/swagger-ui.html`.

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Create user account, provision initial garage slot & balance, return JWT pair |
| `POST` | `/api/v1/auth/login` | Public | Authenticate credentials and issue access/refresh tokens |
| `POST` | `/api/v1/auth/refresh` | Public | Issue a fresh JWT pair using a valid refresh token |
| `GET` | `/api/v1/market/listings` | Authenticated | Retrieve paginated feed of `ACTIVE` listings |
| `POST` | `/api/v1/market/listings` | Authenticated | List an owned vehicle on the market |
| `POST` | `/api/v1/market/listings/{id}/buy` | Authenticated | Atomically purchase a vehicle (transfers funds, ownership, and garage slot) |
| `POST` | `/api/v1/market/listings/{id}/cancel` | Authenticated | Cancel an active listing owned by the requester |
| `POST` | `/api/v1/bot/start/{amount}` | `ROLE_ADMIN` | Sync bot pool in DB and launch virtual-thread simulation workers (`1..999`) |
| `POST` | `/api/v1/bot/stop` | `ROLE_ADMIN` | Gracefully terminate all active simulation workers |

---

## Getting Started

### Prerequisites
* JDK 21+
* Docker & Docker Compose

### Run with Docker Compose
The Compose configuration includes container resource limits (`650MB` / `-Xmx450m` for the application, `300MB` for PostgreSQL) and database healthchecks:

```bash
docker compose up --build -d
```

* **API Base URL:** `http://localhost:8080`
* **Swagger UI:** `http://localhost:8080/swagger-ui.html`

### Local Development
Configure the environment variables (or update `application.properties` / `application.yml`):

```properties
DB_HOST=localhost
APP_SECURITY_JWT_SECRET=<256-bit-base64-or-utf8-secret>
APP_SECURITY_JWT_ACCESS_TOKEN_TTL_MS=900000
APP_SECURITY_JWT_REFRESH_TOKEN_TTL_MS=604800000
APP_FINANCE_INITIAL_BALANCE=1000.00
APP_BOT_INITIAL_SIZE=1000
```

Start the application:

```bash
./gradlew bootRun
```

---

## Testing & CI

Integration and slice tests run against a real **PostgreSQL 16** instance via **Testcontainers** (`postgres:16-alpine`):

* **Repository Slice Tests (`@DataJpaTest`):** Validate database `CHECK` constraints, partial unique indexes, foreign key cascade/restrict rules, pessimistic locks, and custom native/JPQL queries.
* **API & Security Tests (`@SpringBootTest` + `MockMvc` / `TestRestTemplate`):** Verify JWT authentication flows, RBAC enforcement, request validation, and error responses.
* **Concurrency Simulation Tests (`BotServiceIT`):** Validate dynamic bot scaling (500–1500 bots) executing trades concurrently with real user transactions.

Run the full test suite locally:

```bash
./gradlew check
```

Every push and pull request targeting `main` or `develop` runs the **GitHub Actions** pipeline (`Carbuyer CI with Gradle`) executing the full build and integration test suite.