# Order Service

This is an order-service which does placing/filling/cancelling orders, maintaining portfolios. 
It is developed using Spec driven Development using macro spec [`openspec/macro-spec.md`](openspec/macro-spec.md) with Cursor Plan mode 
fine tuned the specs before development, supervised and verified all the changes. More details about this [here](https://martinfowler.com/articles/exploring-gen-ai/sdd-3-tools.html)

## Run

- **JDK:** 21  
- **Database:** MySQL 8
- **Spring boot:** 3.5

```bash
./gradlew bootRun
```

## Tests

```bash
./gradlew test
```

Tests use **H2** in MySQL compatibility mode.

## Mock Data

we can use [`scripts/samples/api-curl-samples.sh`](scripts/samples/api-curl-samples.sh) to generate some mock orders and view the portfolio, overlap.

## API

| Action | Method / path |
|--------|----------------|
| Place order | `POST /orders` |
| Fill order | `POST /orders/{id}/fill` |
| Cancel order | `POST /orders/{id}/cancel` |
| Get portfolio | `GET /portfolios/{traderId}` |
| Sector overlap | `GET /portfolios/{traderId}/sector-overlap` |
| Add to portfolio | `POST /portfolios/{traderId}/positions` |


## Key decisions

1. **Schema & Liquibase dependency:** Added liquibase to maintain the DB versioning and keep the application intact for all users.
2. **Max three `PENDING` orders per trader:** Before counting or inserting, the service **locks the trader row** (`SELECT ... FOR UPDATE` via `TraderAccountService.lockTraderAccount`) so concurrent `POST /orders` calls cannot all pass a naive count check.
3. **Overlap math:** Implemented in [`SectorOverlapCalculator`](src/main/java/com/fynxt/orderservice/overlap/SectorOverlapCalculator.java).
4. **Persistence:** Spring Data JPA matches the existing stack; fill/cancel use pessimistic locks on the order row.

## Intentional skips / deferrals

- Authentication, pagination, and audit history.
- **Testcontainers MySQL** for integration tests (timeboxed; unit tests cover rules and overlap math on H2).
