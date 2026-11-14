# Business Manager Database Scripts

All scripts target the application's own database, `business_manager` (MySQL 8.0+, InnoDB, utf8mb4).
No legacy databases (for example `retailpro`) are referenced anywhere in this project.

## One-time bootstrap (run as MySQL administrator)

```powershell
mysql -u root -p < database/setup.sql
```

`setup.sql` creates:

- the `business_manager` database
- a dedicated application user `business_manager_app@localhost` with rights only on `business_manager`
- grants for schema auto-creation at first launch

## Schema and seed data

- `schema.sql` — full schema: businesses, branches, users, categories, suppliers, customers,
  products, branch_inventory, sales, sale_items, purchases, purchase_items,
  inventory_transactions, payments, expenses, employees.
- `sample_data.sql` — optional seed rows (currently clean; the app seeds its required
  administrator account automatically through `DatabaseInitializer`).

The application connects with the settings in `src/main/resources/db.properties`
(override with `BUSINESS_MANAGER_DB_URL`, `BUSINESS_MANAGER_DB_USER`, `BUSINESS_MANAGER_DB_PASS`
environment variables). On its first successful connection it verifies the schema and, if the
tables are missing, applies `database/schema.sql` itself. If the database itself does not exist
yet, the app now creates it automatically using its server-level connection, so running
`setup.sql` is recommended but not mandatory on developer machines.
