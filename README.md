# Finance Management API

Self-contained Spring Boot REST API for locally managed finances. It uses Java 21, Spring Boot 3.4.2, Spring
Security/JWT, Spring Data JPA, Validation, JUnit 5/Mockito, and an in-memory H2 database. No external service or
database credentials are required.

## Run locally

Install Java 21 and Maven, then start with `mvn spring-boot:run`. H2 is initialized automatically and resets when the
process exits. The H2 console is available at `/h2-console` using `jdbc:h2:mem:finance_management`, username `sa`, blank
password. A development-only user is seeded as `demo` / `demo123`; users can also register at `POST /api/auth/register`.

## Architecture

`api` contains REST adapters and request validation; `service` contains domain workflows and financial rules; `repo`
contains Spring Data persistence; `model` contains JPA entities. Financial account ownership is checked in services for
every operation. Transaction writes and savings contributions run in database transactions; account rows are locked
during balance changes. Ledger deletion is a reversal that preserves the original row. Amounts use `BigDecimal`.

Key service boundaries:

- `AccountService`: account lifecycle, ownership, account-type validation.
- `CategoryService`: system defaults and user-owned category lifecycle. System categories are shared and read-only.
- `TransactionService`: transaction create/update/reversal, ownership, category validation, balance consistency, and
  budget integration.
- `BudgetService`: monthly spending calculations and idempotent threshold notifications.
- `SavingsGoalService`: contributions, goal progress, completion notifications, contribution history.
- `RecurringTransactionService` and `RecurringExecutionWorker`: local scheduled processing with a unique recurrence/date
  constraint and execution status history.
- `AnalyticsService`: actual ledger based cash-flow, monthly/yearly reports, and category/account expense totals.
- `NotificationService`: user-scoped internal notices and read state.

## REST endpoints

Every route below except auth login/register/refresh/logout requires `Authorization: Bearer <accessToken>`.

- Auth: `POST /api/auth/register`, `/login`, `/refresh`, `/logout`; `GET /api/auth/me`; `POST /api/auth/password`;
  `DELETE /api/auth/me`.
- Accounts: `POST/GET /api/accounts`, `GET/PUT/DELETE /api/accounts/{id}`, `GET /api/accounts/{id}/transactions`.
- Categories: `POST/GET /api/categories`, `PUT/DELETE /api/categories/{id}`. Default categories are seeded for food,
  groceries, transportation, rent, utilities, shopping, healthcare, education, entertainment, travel, salary, business,
  freelance, investments, and other income. Types are `INCOME` or `EXPENSE`.
- Ledger: `POST/GET /api/transactions`, `GET/PUT/DELETE /api/transactions/{id}`. Search filters include `from`, `to`,
  `accountId`, `categoryId`, `type`; `page`, `size`, and `sort` support pagination. Transaction types are `INCOME` and
  `EXPENSE`.
- Budgets: `POST/GET /api/budgets`, `POST /api/budgets/yearly`, `GET/PUT/DELETE /api/budgets/{id}`,
  `PUT /api/budgets/{id}/yearly`. Budgets can target an expense category or all spending for a selected month or year.
  Responses include spent, remaining, percent used, and overspend status.
- Savings: `POST/GET /api/savings-goals`, `GET/PUT/DELETE /api/savings-goals/{id}`,
  `POST /api/savings-goals/{id}/contributions`, `GET /api/savings-goals/{id}/contributions`.
- Recurring: `POST/GET /api/recurring-transactions`, `GET /api/recurring-transactions/{id}/executions`,
  `PATCH /api/recurring-transactions/{id}/pause`, `/resume`, `DELETE /api/recurring-transactions/{id}`. Frequency:
  `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`. A local Spring scheduled task polls due definitions once a minute; failures
  are kept in execution history.
- Notifications: `GET /api/notifications`, `/unread`; `PATCH /api/notifications/{id}/read`, `/read-all`;
  `DELETE /api/notifications/{id}`.
- Dashboard: `GET /api/dashboard/summary`, `/monthly?year=2026`, `/expenses-by-category`.
- Reports: `GET /api/reports/monthly?year=2026&month=11`, `/yearly?year=2026`, `/expenses-by-account`,
  `/transactions.csv`.

Account types: `SAVINGS`, `CURRENT`, `CASH`, `CREDIT_CARD`, `OTHER`. On credit cards, expenses increase the outstanding
balance and income reduces it. Non-credit accounts reject an expense that would make the balance negative.

## Example

1. Register or log in and use the returned access token as Bearer authorization.
2. Create an account: `POST /api/accounts` with
   `{"name":"Everyday","type":"SAVINGS","currency":"INR","openingBalance":25000.00}`.
3. Create an expense category: `POST /api/categories` with `{"name":"Food","type":"EXPENSE"}`.
4. Record an expense: `POST /api/transactions` with
   `{"accountId":1,"categoryId":1,"type":"EXPENSE","amount":2000.00,"transactionDate":"2026-11-21","description":"Groceries"}`.
5. Create a budget: `POST /api/budgets` with `{"categoryId":1,"year":2026,"month":11,"limitAmount":8000.00}`. Threshold
   notifications appear at 80% and 100%.
6. Create a recurring rent entry by posting to `/api/recurring-transactions` with type `EXPENSE`, frequency `MONTHLY`,
   and the account/category IDs.

## Tests

Tests use JUnit 5 and Mockito for isolated service rules, plus Spring MockMvc integration tests for authentication and
finance API ownership/integration. They are part of the standard test source tree under `src/test/java`.
