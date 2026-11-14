# Stitch screen to application mapping

| Stitch screen | Swing screen | Service / DAO | Primary MySQL tables |
| --- | --- | --- | --- |
| Welcome | `ui/WelcomePanel` | — | — |
| Login | `ui/auth/LoginPanel` | `AuthService`, `UserDAO`, `BusinessDAO` | `users`, `businesses`, `branches` |
| Create account | `ui/auth/CreateAccountPanel` | `AuthService`, `UserDAO` | `users` |
| Business information | `ui/auth/BusinessInfoPanel` | `AuthService`, `BusinessDAO`, `BranchDAO` | `businesses`, `branches` |
| Business type selection | `ui/auth/BusinessTypePanel` | `AuthService`, `BusinessDAO` | `businesses` |
| Small-business dashboard | `ui/dashboard/SmallBusinessDashboardPanel` | `AnalyticsService`, `ReportService` | `sales`, `products`, `branch_inventory`, `inventory_transactions` |
| Enterprise dashboard | `ui/dashboard/LargeBusinessDashboardPanel` | `AnalyticsService`, `ReportService` | `sales`, `products`, `branch_inventory`, `inventory_transactions` |
| Product management | `ui/products/ProductManagementPanel` | `ProductService`, `ProductDAO` | `products`, `branch_inventory` |
| Inventory management | `ui/inventory/InventoryManagementPanel` | `ProductService`, `ProductDAO` | `products`, `branch_inventory`, `inventory_transactions` |
| Customer management | `ui/customers/CustomerManagementPanel` | `CustomerService`, `CustomerDAO` | `customers`, `payments`, `sales` |
| Supplier management | `ui/suppliers/SupplierManagementPanel` | `SupplierService`, `SupplierDAO` | `suppliers`, `payments`, `purchases` |
| POS billing | `ui/pos/POSBillingPanel` | `ProductService`, `SaleService`, `SaleDAO` | `products`, `branch_inventory`, `sales`, `sale_items`, `payments`, `inventory_transactions` |

Additional application modules (`Categories`, `Purchases`, `Payments`, `Expenses`, `Employees`, `Branches`, `Reports`, `Analytics`, `Settings`, and `Sales History`) use the same Stitch Executive Dark Insight tokens and are reachable from `MainFrame` navigation.

## Verification rule

The supplied `executive_dark_insight/DESIGN.md` is the token source for all mapped screens: fixed 240px sidebar, 24px content padding, 16px gutters, dark surface layers, Inter typography, 8px/12px component radii, and 48px standard rows.
