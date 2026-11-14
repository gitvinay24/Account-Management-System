# Business Manager — Verified Status

## Last verification

**2026-09-24** — migrated the project to its own dedicated `business_manager` database (bootstrap script `database/setup.sql`, dedicated application user, auto-creation on first launch). All `retailpro` database references were removed from configuration and documentation.

**2026-09-05, 15:45 IST** — configured local MySQL, Java 17 build.

## Completed and verified

- [x] **Configured MySQL connection and schema initialization:** `DatabaseInitializer` connected successfully and verified the schema during the clean test run and the real GUI run.
- [x] **Authentication and session handling:** valid account login, rejected unknown/wrong-password logins, PBKDF2 password hashing, logout, and restart persistence passed.
- [x] **Authorization:** navigation and service-level permission tests passed for authorized and unauthorized scenarios.
- [x] **CRUD and branch inventory:** products, categories, customers, suppliers, branches, purchases, sales, and inventory movement were verified against MySQL.
- [x] **Financial transaction integrity:** purchase and sale workflows use database transactions. A cash overpayment now records the applied amount only and stores change separately; its ledger record is verified by `MySqlIntegrationTest`.
- [x] **Payments:** customer sale settlement and document-status updates are exercised against MySQL.
- [x] **Reports and analytics:** live report rows and the top-products analytics dataset were verified after a live sale.
- [x] **Invoice preview and printing:** preview modal and cancellation handling passed. Explicit PDF destinations now produce a valid, non-empty PDF reliably; this avoids Windows `Microsoft Print to PDF` ignoring Java's destination attribute while normal physical-printer jobs continue through `PrinterJob`.
- [x] **Safe integration-test cleanup:** active integration tests create a unique business and delete only that ID with a `PreparedStatement`. The old broad pattern-based cleanup test is disabled.
- [x] **No production credential fallback in code:** `DBConnection` reads the existing configuration or environment variables and produces a clear error if configuration is incomplete.
- [x] **Dedicated application database:** the app targets `business_manager` (see `database/setup.sql`), creates it automatically on first launch, and no longer references the legacy `retailpro` database or its user.
- [x] **Startup resilience:** if MySQL is unreachable at launch, `MainFrame` shows a retry/exit dialog instead of opening a window that fails on every click; uncaught EDT exceptions surface as readable dialogs.

## Verification commands and outcomes

| Check | Result |
| --- | --- |
| `mvn clean test` | **PASS** — 17 tests, 0 failures, 0 errors, 0 skipped |
| `-DBUSINESS_MANAGER_IT=true -Dtest=RealGuiManualVerificationRunner test` | **PASS** — MySQL login, CRUD, purchase stock increase, POS stock decrease, payment/change, invoice preview, PDF output, reports, analytics, and restart persistence |

The GUI verification generated valid invoice PDF artifacts in `scratch/` during its run. Credentials are not recorded in this file.

## Notes

- The Swing Settings screen includes a **Database & System** card with the configured connection target and a live **Test Connection** check.

## Remaining acceptance work

- [ ] **Pixel-level visual comparison against every supplied Stitch screen** has not been re-run as part of the 2026-09-05 backend verification. It must not be marked verified without a separate visual acceptance pass.

## Notes

- Application runs in live JDBC/MySQL mode by default. Assessment/demo mode is opt-in through `ASSESSMENT_MODE=true` or `DEMO_MODE_ENABLED=true`.
- MySQL integration tests require `BUSINESS_MANAGER_IT=true` and should use a disposable database or the explicitly configured local development database, never production data.
