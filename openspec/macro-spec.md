# SDD: Order booking & portfolio

**Purpose:** Single reference for *what* the feature does and *why* it is shaped this way—useful when walking an interviewer through scope, tradeoffs, and correctness.

**Implementation:** Spring Boot 3.5, Java 21, JPA, MySQL + Liquibase (`schema.sql`), overlap logic in plain Java.

---

## 1. Problem statement

Traders submit **BUY/SELL** orders that move through a simple lifecycle. The system must enforce **portfolio and flow rules** under concurrency, expose a **portfolio view**, and report **benchmark overlap** against three fixed equity baskets—without pushing overlap math into SQL or Spring.

---

## 2. Goals & non-goals

### In scope

- REST API for orders (place, fill, cancel), portfolio read, manual position add, and sector-overlap read.
- Relational persistence with **explicit DDL** and migrations.
- **Pure Java** overlap calculation (testable without the framework).
- Clear **4xx** errors for rule violations and invalid transitions.

### Out of scope (document for interviewer)

- Authentication / authorization, idempotency keys, pagination, full audit trail.
- Market data, pricing, partial fills, order books.
- Testcontainers-backed MySQL in CI (unit tests use H2 + Hibernate `ddl-auto` instead).

---

## 3. Domain concepts

| Concept | Description |
|--------|-------------|
| **Trader** | Logical actor identified by `traderId` (string). A durable `traders` row exists for locking and FK integrity. |
| **Order** | Instruction: ticker (`stock`), reporting `sector`, `quantity`, `side` (BUY/SELL), `status` (PENDING → FILLED or CANCELLED). |
| **Position** | One row per `(traderId, stock)` with aggregate `quantity` and last-written `sector` (for reporting / sector totals). |
| **Basket** | Static benchmark **set of tickers** (not the trader’s sector labels). Used only for overlap. |

---

## 4. Business rules (normative)

1. **Order lifecycle:** Only **PENDING** orders may be **filled** or **cancelled**. Terminal states: **FILLED**, **CANCELLED**.
2. **Pending cap:** At most **three** orders with status **PENDING** per `traderId` at any time.
3. **SELL placement:** A new **SELL** order is rejected if the trader’s current position quantity for that `stock` is **strictly less** than the order quantity (no short selling on place).
4. **Fill — BUY:** On fill, **increase** position quantity for that stock (create row or merge); update stored `sector` to the order’s sector (last write wins).
5. **Fill — SELL:** On fill, **decrease** position quantity; reject if insufficient quantity at fill time (guards races vs. concurrent adds/fills).
6. **Add to portfolio:** Upsert `(traderId, stock)`—add quantity, set `sector` from request (same merge semantics as incremental adds).

---

## 5. Concurrency & consistency (how you explain “correct under load”)

- **Pending cap:** Before counting PENDING orders or inserting a new one, the service **locks the trader row** (`SELECT … FOR UPDATE` via JPA). All competing `POST /orders` for that trader serialize on that lock, so **count + insert** is atomic in one transaction.
- **Fill / cancel:** The **order row** is loaded with a **pessimistic write lock** so two threads cannot both “fill” the same PENDING order.
- **Isolation:** Default transactional boundaries rely on the database (MySQL InnoDB **REPEATABLE READ** by default). Document that **lock ordering** is “trader lock on place; order lock on fill/cancel” to reduce deadlock risk in extensions.

---

## 6. REST surface (macro)

| Capability | HTTP | Notes |
|------------|------|--------|
| Place order | `POST /orders` | Body: `traderId`, `stock`, `sector`, `quantity`, `side` (`BUY` / `SELL`). |
| Fill order | `POST /orders/{id}/fill` | Idempotent-ish only if callers never retry blindly—**no idempotency key** in v1. |
| Cancel order | `POST /orders/{id}/cancel` | |
| Portfolio | `GET /portfolios/{traderId}` | Positions map + **sector totals** (sum of quantities by `sector` string on positions). |
| Sector overlap | `GET /portfolios/{traderId}/sector-overlap` | See §7. |
| Add position | `POST /portfolios/{traderId}/positions` | Body: `stock`, `sector`, `quantity`. |

**Errors:** JSON `{"error":"…"}`. Use **400** for rule/validation violations, **404** when an order id does not exist.

---

## 7. Sector overlap (normative algorithm)

### 7.1 Inputs

- Build **P**, the **set of distinct tickers** held with **quantity &gt; 0** (share counts **do not** weight overlap).
- **Sector totals and position quantities are not inputs** to overlap—only **which tickers** are held.

### 7.2 Static baskets (ticker sets)

| Basket id | Tickers |
|-----------|---------|
| `TECH_HEAVY` | AAPL, MSFT, GOOGL, TSLA, NVDA |
| `FINANCE_HEAVY` | JPM, GS, BAC, MS, WFC |
| `BALANCED` | AAPL, JPM, XOM, JNJ, TSLA |

### 7.3 Overlap per basket

Let **C** = \|P ∩ B\| (count of tickers in both portfolio set **P** and basket **B**).  
Let **p** = \|P\|, **b** = \|B\|.

\[
\text{overlap\_percent} = \frac{2 \times C}{p + b} \times 100
\]

Rounded to **two decimal places** (half-up). If **p + b = 0**, define overlap as **0.00%**.

### 7.4 Dominant basket

- Choose the basket with the **maximum** overlap percentage.
- **Tie-break:** smallest basket id in **lexicographic** order (e.g. all zeros → `BALANCED`).

### 7.5 Risk flag

- **HIGH** if **any** overlap ≥ **60.00%**
- Else **MEDIUM** if **any** overlap ≥ **40.00%**
- Else **LOW**

### 7.6 Interview clarification (common confusion)

A portfolio of **{AAPL, JPM}** overlaps **both** tickers with **BALANCED** (that basket’s definition includes AAPL and JPM), so **BALANCED** often beats TECH-only or FINANCE-only baskets **even when** sector totals look “tech vs finance heavy”—because overlap is **not** sector-weighted.

---

## 8. Data model (macro)

- **`traders(trader_id PK, created_at)`** — exists to support FKs and **row-level lock** for pending cap.
- **`orders`** — FK to `traders`; CHECK constraints on `quantity > 0`, allowed `side` / `status`; optional optimistic `@Version`.
- **`positions`** — UNIQUE `(trader_id, stock)`; CHECK `quantity >= 0`.

**Source of truth for DDL:** `src/main/resources/schema.sql`, applied once via Liquibase changelog `db/changelog/db.changelog-master.yaml`.

---

## 9. Testing strategy (what to say in interview)

- **Pure overlap:** Unit tests on `SectorOverlapCalculator` (formula, ties, risk bands) **without** Spring.
- **Rules:** Spring `@SpringBootTest` + `@Transactional` tests for pending cap, SELL validation, fill/cancel guards (H2 profile).
- **Gap you can name:** No Testcontainers MySQL e2e in CI—acceptable tradeoff for timeboxed take-home; production validation uses real DDL + Liquibase.

---

## 10. Code map (for quick navigation)

| Area | Location |
|------|----------|
| Overlap (pure Java) | `src/main/java/com/fynxt/orderservice/overlap/` |
| Spring bean wiring for calculator | `src/main/java/com/fynxt/orderservice/config/OverlapConfiguration.java` |
| Order rules | `src/main/java/com/fynxt/orderservice/service/OrderService.java` |
| Portfolio + overlap endpoint orchestration | `src/main/java/com/fynxt/orderservice/service/PortfolioService.java` |
| Trader lock helper | `src/main/java/com/fynxt/orderservice/service/TraderAccountService.java` |
| REST | `…/controller/OrderController.java`, `…/controller/PortfolioController.java` |
| Errors | `…/api/error/` |
| DDL + Liquibase | `src/main/resources/schema.sql`, `src/main/resources/db/changelog/` |

---

## 11. Elevator lines (sound bites)

- “**Overlap is set-based on tickers**, not weighted by quantity or sector tags—so it answers ‘how similar is my *holdings universe* to each benchmark index’, not ‘what’s my sector mix’.”
- “**Pending cap is safe under concurrency** because we **lock the trader row** before count+insert.”
- “**Overlap is isolated** in a **Spring-free** class so the rule is easy to unit test and doesn’t drift with ORM queries.”
