# Account Management

Account Management is a Java 17 desktop application for small shops and multi-branch businesses. It uses Swing, FlatLaf, JDBC, MySQL, and JFreeChart, with a focused workspace for accounts, payments, operations, and reporting.

## Included modules

- Secure login, multi-step registration, password changes, sessions, and roles
- Business-mode-specific navigation for small and large businesses
- Product, category, inventory, supplier, customer, expense, employee, and branch management
- Purchases and POS sales with transactional inventory updates
- Customer and supplier payment-ledger entries created with sales and purchases
- Database-backed dashboards, reports, analytics, CSV report export, and JFreeChart views

## Requirements

- JDK 17 or later
- Maven 3.9 or later
- MySQL 8.0+ for production mode (not required for assessment mode)

## Database setup

The application uses its own database, `business_manager` (no legacy `retailpro` database is involved).

1. Run the one-time bootstrap script as a MySQL administrator:

   ```powershell
   mysql -u root -p < database/setup.sql
   ```

   This creates the `business_manager` database and a dedicated application user. (Developer shortcut: the app also creates the database automatically on first launch if it is missing.)
2. Edit `src/main/resources/db.properties` if your local credentials differ, or set `BUSINESS_MANAGER_DB_URL`, `BUSINESS_MANAGER_DB_USER`, and `BUSINESS_MANAGER_DB_PASS` environment variables.
3. Start the application. On its first successful database connection, it creates the schema from `database/schema.sql` and the required administrator account.

The development sample users use the password `Password@123`; change or remove them before any non-development deployment.

## Build and run

```powershell
mvn test
mvn package
java -cp target/account-management-1.0-SNAPSHOT.jar com.businessmanager.Main
```

Run the application from the project root so its first-run database scripts are available. See `database/README.md` for schema notes.

### Assessment mode (offline faculty demonstration)

The normal default is live JDBC/MySQL persistence. Assessment mode is opt-in: set `ASSESSMENT_MODE=true` (or the legacy `DEMO_MODE_ENABLED=true`) as a JVM property or environment variable to use the in-memory demonstration data instead.

### MySQL integration tests

The real-database tests are gated by `BUSINESS_MANAGER_IT=true`. Configure `db.properties` (or the three `BUSINESS_MANAGER_DB_*` environment variables) for a disposable MySQL database containing this project's schema, then run `mvn test`. The suite creates a uniquely named business and removes only that business through a parameterized cleanup step. Do not enable it against production data.

## Project layout

`ui/` contains Swing screens, `service/` coordinates business logic, `dao/` owns JDBC persistence, `model/` holds entities, `theme/` and `components/` provide shared Stitch styling, and `database/` holds MySQL scripts (`setup.sql` bootstrap, `schema.sql`, `sample_data.sql`).
