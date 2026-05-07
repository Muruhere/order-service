# Order Service (trading desk take-home)

This is an order-service which does placing/filling/cancelling orders, maintaining portfolios. 
It is developed in spec-driven deployment using macro spec [`openspec/macro-spec.md`](openspec/macro-spec.md) with Cursor Plan mode 
fine tuned the specs before development. More details about spec-driven development [here](https://martinfowler.com/articles/exploring-gen-ai/sdd-3-tools.html)

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

Tests use **H2** in MySQL compatibility mode with **Liquibase disabled** and `spring.jpa.hibernate.ddl-auto=create-drop` so CI does not require MySQL. Production-like DDL remains in `src/main/resources/schema.sql` and is applied via Liquibase against MySQL.

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

1. **Schema:** [`src/main/resources/schema.sql`](src/main/resources/schema.sql) is the reviewed DDL (constraints, FKs). [**Liquibase**](src/main/resources/db/changelog/db.changelog-master.yaml) runs that file once; `spring.jpa.hibernate.ddl-auto=validate` keeps the JPA model aligned with the database.
2. **Liquibase dependency:** `org.liquibase:liquibase-core` is **`runtimeOnly`** in Gradle so the app does not compile against Liquibase APIs.
3. **Max three `PENDING` orders per trader:** Before counting or inserting, the service **locks the trader row** (`SELECT ... FOR UPDATE` via `TraderAccountService.lockTraderAccount`) so concurrent `POST /orders` calls cannot all pass a naive count check.
4. **Overlap math:** Implemented in [`SectorOverlapCalculator`](src/main/java/com/fynxt/orderservice/overlap/SectorOverlapCalculator.java) with **no Spring imports**; Spring wires it through [`OverlapConfiguration`](src/main/java/com/fynxt/orderservice/config/OverlapConfiguration.java). Dominant basket uses the **highest overlap**, ties broken by **lexicographically smallest** basket name.
5. **Persistence:** Spring Data JPA matches the existing stack; fill/cancel use pessimistic locks on the order row.

## Intentional skips / deferrals

- Authentication, pagination, and audit history.
- **Testcontainers MySQL** for integration tests (timeboxed; unit tests cover rules and overlap math on H2).
