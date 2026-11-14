package com.businessmanager.integration;

import com.businessmanager.database.DBConnection;
import com.businessmanager.database.DatabaseInitializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Disabled("Unsafe legacy broad-pattern cleanup. Integration tests now create and delete only their own uniquely identified business data.")
@EnabledIfEnvironmentVariable(named = "BUSINESS_MANAGER_IT", matches = "true")
public class DatabaseProductionCleanup {

    @BeforeAll
    static void setup() {
        assertTrue(DatabaseInitializer.initializeDatabase(), "Database initialization failed");
    }

    @Test
    void executeProductionDataCleanupAndBranchInventoryRepair() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");

            // Delete test-generated sale items, sales, payments
            stmt.executeUpdate("DELETE FROM sale_items WHERE sale_id IN (SELECT id FROM sales WHERE notes LIKE '%Test%' OR notes LIKE '%QA%' OR invoice_number LIKE '%TEST%' OR invoice_number LIKE '%INV-%') OR product_id IN (SELECT id FROM products WHERE name LIKE '%Test%' OR name LIKE '%Widget%' OR name LIKE '%Laptop%')");
            stmt.executeUpdate("DELETE FROM sales WHERE notes LIKE '%Test%' OR notes LIKE '%QA%' OR invoice_number LIKE '%TEST%' OR invoice_number LIKE '%INV-%'");
            stmt.executeUpdate("DELETE FROM payments WHERE reference_number LIKE '%TEST%' OR notes LIKE '%Test%'");

            // Delete test-generated purchase items, purchases
            stmt.executeUpdate("DELETE FROM purchase_items WHERE purchase_id IN (SELECT id FROM purchases WHERE notes LIKE '%Test%' OR notes LIKE '%QA%') OR product_id IN (SELECT id FROM products WHERE name LIKE '%Test%' OR name LIKE '%Widget%')");
            stmt.executeUpdate("DELETE FROM purchases WHERE notes LIKE '%Test%' OR notes LIKE '%QA%'");

            // Delete test inventory transactions
            stmt.executeUpdate("DELETE FROM inventory_transactions WHERE notes LIKE '%Test%' OR notes LIKE '%Widget%'");

            // Delete test products, branch_inventory
            stmt.executeUpdate("DELETE FROM branch_inventory WHERE product_id IN (SELECT id FROM products WHERE name LIKE '%Test%' OR name LIKE '%Widget%' OR sku LIKE '%DTW-%' OR sku LIKE '%TEST-%')");
            stmt.executeUpdate("DELETE FROM products WHERE name LIKE '%Test%' OR name LIKE '%Widget%' OR sku LIKE '%DTW-%' OR sku LIKE '%TEST-%'");

            // Delete test customers, suppliers, expenses
            stmt.executeUpdate("DELETE FROM customers WHERE name LIKE '%Test%' OR name LIKE '%QA%'");
            stmt.executeUpdate("DELETE FROM suppliers WHERE name LIKE '%Test%' OR name LIKE '%QA%'");
            stmt.executeUpdate("DELETE FROM expenses WHERE title LIKE '%Test%' OR description LIKE '%Test%'");

            // Auto-repair branch_inventory for all remaining valid products
            String repairSql = "INSERT INTO branch_inventory (branch_id, product_id, quantity, min_stock) " +
                    "SELECT b.id, p.id, p.stock_quantity, p.min_stock " +
                    "FROM products p " +
                    "JOIN branches b ON b.business_id = p.business_id AND b.is_main = 1 " +
                    "ON DUPLICATE KEY UPDATE quantity = IF(branch_inventory.quantity = 0 AND p.stock_quantity > 0, p.stock_quantity, branch_inventory.quantity)";
            stmt.executeUpdate(repairSql);

            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("==================================================");
            System.out.println("PRODUCTION DATA CLEANUP & BRANCH REPAIR SUCCESSFUL");
            System.out.println("==================================================");
        }
    }
}
