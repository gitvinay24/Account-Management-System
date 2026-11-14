package com.businessmanager.integration;

import com.businessmanager.dao.*;
import com.businessmanager.database.DBConnection;
import com.businessmanager.database.DatabaseInitializer;
import com.businessmanager.enums.*;
import com.businessmanager.model.*;
import com.businessmanager.security.AccessControl;
import com.businessmanager.security.PasswordHasher;
import com.businessmanager.security.UserSession;
import com.businessmanager.service.AuthService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "BUSINESS_MANAGER_IT", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BackendCompleteAuditMatrixTest {

    private static String token;
    private static int businessId;
    private static int branchId;
    private static int userId;
    private static int categoryId;
    private static int supplierId;
    private static int customerId;
    private static int productId;
    private static int purchaseId;
    private static int saleId;

    @BeforeAll
    static void setUpAll() throws Exception {
        assertTrue(DatabaseInitializer.initializeDatabase(), "Database initialization failed");
        token = UUID.randomUUID().toString().substring(0, 8);

        // Register a test business and owner
        Business b = new Business();
        b.setName("Audit Business " + token);
        b.setEmail("audit-" + token + "@example.test");
        b.setType(BusinessType.LARGE);

        User u = new User();
        u.setFullName("Audit Admin");
        u.setUsername("audit_" + token);
        u.setEmail("audit_user_" + token + "@example.test");
        u.setPhone("9876543210");

        AuthService auth = new AuthService();
        AuthService.AuthResult res = auth.registerBusinessAndUser(u, b, "Audit#Pass2026".toCharArray());
        assertTrue(res.isSuccess(), "Registration failed: " + res.getMessage());

        businessId = b.getId();
        userId = u.getId();

        assertTrue(auth.login(u.getUsername(), "Audit#Pass2026").isSuccess(), "Login failed");
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        UserSession.clearSession();
        if (businessId > 0) {
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    deleteByBiz(conn, "DELETE pi FROM purchase_items pi JOIN purchases p ON p.id = pi.purchase_id WHERE p.business_id = ?");
                    deleteByBiz(conn, "DELETE si FROM sale_items si JOIN sales s ON s.id = si.sale_id WHERE s.business_id = ?");
                    deleteByBiz(conn, "DELETE FROM businesses WHERE id = ?");
                    conn.commit();
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }
    }

    private static void deleteByBiz(Connection conn, String sql) throws Exception {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            pstmt.executeUpdate();
        }
    }

    @Test
    @Order(1)
    @DisplayName("01. Database Connection Audit")
    void test01DatabaseConnection() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "Connection must not be null");
            assertFalse(conn.isClosed(), "Connection must be open");
            assertTrue(conn.isValid(2), "Connection must be valid");
        }
    }

    @Test
    @Order(2)
    @DisplayName("02. Schema Audit")
    void test02SchemaAudit() throws Exception {
        String[] requiredTables = {
                "businesses", "branches", "users", "categories", "suppliers",
                "customers", "products", "branch_inventory", "sales", "sale_items",
                "purchases", "purchase_items", "inventory_transactions", "payments",
                "expenses", "employees"
        };
        try (Connection conn = DBConnection.getConnection()) {
            for (String table : requiredTables) {
                try (PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1")) {
                    assertDoesNotThrow(() -> pstmt.executeQuery(), "Table missing: " + table);
                }
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("03. Authentication Audit")
    void test03Authentication() {
        AuthService auth = new AuthService();
        // Valid login
        AuthService.AuthResult res = auth.login("audit_" + token, "Audit#Pass2026");
        assertTrue(res.isSuccess(), "Valid login failed");

        // Invalid password
        AuthService.AuthResult badPass = auth.login("audit_" + token, "WrongPassword!");
        assertFalse(badPass.isSuccess(), "Invalid password should fail");

        // Non-existent user
        AuthService.AuthResult noUser = auth.login("non_existent_user_999", "Audit#Pass2026");
        assertFalse(noUser.isSuccess(), "Non-existent user should fail");
    }

    @Test
    @Order(4)
    @DisplayName("04. Authorization Audit")
    void test04Authorization() {
        assertDoesNotThrow(() -> AccessControl.requireWrite("PRODUCTS"));
        assertDoesNotThrow(() -> AccessControl.requireWrite("POS"));
        assertDoesNotThrow(() -> AccessControl.requireWrite("PURCHASES"));

        // Simulate CASHIER role session
        User cashier = new User();
        cashier.setId(999);
        cashier.setRole(UserRole.CASHIER);
        Business b = new Business();
        b.setId(businessId);
        UserSession.startSession(cashier, b);

        assertThrows(SecurityException.class, () -> AccessControl.requireWrite("PRODUCTS"));
        assertThrows(SecurityException.class, () -> AccessControl.requireWrite("EMPLOYEES"));

        // Re-authenticate owner
        User owner = new User();
        owner.setId(userId);
        owner.setRole(UserRole.OWNER);
        UserSession.startSession(owner, b);
    }

    @Test
    @Order(5)
    @DisplayName("05. Branch CRUD Audit")
    void test05BranchCrud() throws Exception {
        Branch branch = new Branch();
        branch.setBusinessId(businessId);
        branch.setName("Audit Branch " + token);
        branch.setCode("BR-AUDIT-" + token);
        branch.setCity("Bengaluru");
        branch.setMain(true);
        branch.setActive(true);

        BranchDAO dao = new BranchDAO();
        branchId = dao.create(branch);
        assertTrue(branchId > 0, "Branch ID should be > 0");

        Branch fetched = dao.findById(branchId);
        assertNotNull(fetched, "Branch should exist");
        assertEquals("Audit Branch " + token, fetched.getName());

        fetched.setName("Audit Branch Updated " + token);
        assertTrue(dao.update(fetched), "Branch update should succeed");
    }

    @Test
    @Order(6)
    @DisplayName("06. Category CRUD Audit")
    void test06CategoryCrud() throws Exception {
        Category c = new Category();
        c.setBusinessId(businessId);
        c.setName("Audit Category " + token);
        c.setDescription("Testing category CRUD");

        CategoryDAO dao = new CategoryDAO();
        categoryId = dao.create(c);
        assertTrue(categoryId > 0, "Category ID should be > 0");

        Category fetched = dao.findById(categoryId);
        assertNotNull(fetched, "Category should exist");

        fetched.setDescription("Updated description");
        assertTrue(dao.update(fetched), "Category update should succeed");
    }

    @Test
    @Order(7)
    @DisplayName("07. Supplier CRUD Audit")
    void test07SupplierCrud() throws Exception {
        Supplier s = new Supplier();
        s.setBusinessId(businessId);
        s.setName("Audit Supplier " + token);
        s.setEmail("supplier-" + token + "@example.test");
        s.setPhone("+91 99999 88888");
        s.setTaxId("GSTIN-SUP-" + token);

        SupplierDAO dao = new SupplierDAO();
        supplierId = dao.create(s);
        assertTrue(supplierId > 0, "Supplier ID should be > 0");

        Supplier fetched = dao.findById(supplierId);
        assertNotNull(fetched, "Supplier should exist");
        assertEquals("GSTIN-SUP-" + token, fetched.getTaxId());
    }

    @Test
    @Order(8)
    @DisplayName("08. Customer CRUD Audit")
    void test08CustomerCrud() throws Exception {
        Customer cust = new Customer();
        cust.setBusinessId(businessId);
        cust.setName("Audit Customer " + token);
        cust.setEmail("customer-" + token + "@example.test");
        cust.setPhone("+91 98888 77777");

        CustomerDAO dao = new CustomerDAO();
        customerId = dao.create(cust);
        assertTrue(customerId > 0, "Customer ID should be > 0");

        Customer fetched = dao.findById(customerId);
        assertNotNull(fetched, "Customer should exist");
    }

    @Test
    @Order(9)
    @DisplayName("09. Employee CRUD Audit")
    void test09EmployeeCrud() throws Exception {
        Employee emp = new Employee();
        emp.setBusinessId(businessId);
        emp.setBranchId(branchId);
        emp.setEmployeeCode("EMP-" + token);
        emp.setFullName("Audit Staff");
        emp.setEmail("staff-" + token + "@example.test");
        emp.setPosition("Sales Executive");
        emp.setDepartment("Sales");
        emp.setSalary(new BigDecimal("35000.00"));
        emp.setHireDate(new Date(System.currentTimeMillis()));

        EmployeeDAO dao = new EmployeeDAO();
        int empId = dao.create(emp);
        assertTrue(empId > 0, "Employee ID should be > 0");

        Employee fetched = dao.findById(empId);
        assertNotNull(fetched, "Employee should exist");

        assertTrue(dao.delete(empId), "Employee deletion should succeed");
    }

    @Test
    @Order(10)
    @DisplayName("10. Product CRUD Audit")
    void test10ProductCrud() throws Exception {
        Product p = new Product();
        p.setBusinessId(businessId);
        p.setCategoryId(categoryId);
        p.setSupplierId(supplierId);
        p.setName("Audit Product " + token);
        p.setSku("SKU-AUDIT-" + token);
        p.setPurchasePrice(new BigDecimal("100.00"));
        p.setSellingPrice(new BigDecimal("150.00"));
        p.setStockQuantity(20);
        p.setMinStock(5);
        p.setUnit("pcs");
        p.setTaxRate(new BigDecimal("18.00"));

        ProductDAO dao = new ProductDAO();
        productId = dao.create(p);
        assertTrue(productId > 0, "Product ID should be > 0");

        Product fetched = dao.findById(productId);
        assertNotNull(fetched, "Product should exist");
        assertEquals(20, fetched.getStockQuantity());

        assertTrue(dao.existsBySku(businessId, "SKU-AUDIT-" + token, null), "SKU existence check should return true");
    }

    @Test
    @Order(11)
    @DisplayName("11. Purchase Transaction Audit")
    void test11PurchaseTransaction() throws Exception {
        Purchase purchase = new Purchase();
        purchase.setBusinessId(businessId);
        purchase.setBranchId(branchId);
        purchase.setSupplierId(supplierId);
        purchase.setUserId(userId);
        purchase.setPurchaseNumber("PO-AUDIT-" + token);
        purchase.setSubtotal(new BigDecimal("1000.00"));
        purchase.setTotal(new BigDecimal("1000.00"));
        purchase.setPaidAmount(new BigDecimal("1000.00"));
        purchase.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        purchase.setPaymentStatus(PaymentStatus.PAID);

        PurchaseItem item = new PurchaseItem();
        item.setProductId(productId);
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setLineTotal(new BigDecimal("1000.00"));
        purchase.getItems().add(item);

        PurchaseDAO dao = new PurchaseDAO();
        assertTrue(dao.createPurchase(purchase), "Purchase creation must succeed");
        purchaseId = purchase.getId();
        assertTrue(purchaseId > 0, "Purchase ID should be > 0");

        // Stock quantity should now be 20 initial + 10 purchase = 30
        Product updatedProduct = new ProductDAO().findById(productId);
        assertEquals(30, updatedProduct.getStockQuantity(), "Stock should increase after purchase");
    }

    @Test
    @Order(12)
    @DisplayName("12. POS Sale Transaction Audit")
    void test12SaleTransaction() throws Exception {
        Sale sale = new Sale();
        sale.setBusinessId(businessId);
        sale.setBranchId(branchId);
        sale.setUserId(userId);
        sale.setCustomerId(customerId);
        sale.setInvoiceNumber("INV-AUDIT-" + token);
        sale.setSubtotal(new BigDecimal("750.00"));
        sale.setTotal(new BigDecimal("750.00"));
        sale.setPaidAmount(new BigDecimal("750.00"));
        sale.setPaymentMethod(PaymentMethod.UPI);
        sale.setPaymentStatus(PaymentStatus.PAID);

        SaleItem item = new SaleItem();
        item.setProductId(productId);
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("150.00"));
        item.setLineTotal(new BigDecimal("750.00"));
        sale.getItems().add(item);

        SaleDAO dao = new SaleDAO();
        assertTrue(dao.createSale(sale), "Sale creation must succeed");
        saleId = sale.getId();
        assertTrue(saleId > 0, "Sale ID should be > 0");

        // Stock quantity should now be 30 - 5 = 25
        Product updatedProduct = new ProductDAO().findById(productId);
        assertEquals(25, updatedProduct.getStockQuantity(), "Stock should decrease after POS sale");
    }

    @Test
    @Order(13)
    @DisplayName("13. Insufficient Stock Prevention Audit")
    void test13InsufficientStockPrevention() {
        Sale excessiveSale = new Sale();
        excessiveSale.setBusinessId(businessId);
        excessiveSale.setBranchId(branchId);
        excessiveSale.setUserId(userId);
        excessiveSale.setCustomerId(customerId);
        excessiveSale.setInvoiceNumber("INV-EXCESS-" + token);
        excessiveSale.setSubtotal(new BigDecimal("150000.00"));
        excessiveSale.setTotal(new BigDecimal("150000.00"));
        excessiveSale.setPaidAmount(new BigDecimal("150000.00"));

        SaleItem item = new SaleItem();
        item.setProductId(productId);
        item.setQuantity(1000); // Exceeds available 25
        item.setUnitPrice(new BigDecimal("150.00"));
        item.setLineTotal(new BigDecimal("150000.00"));
        excessiveSale.getItems().add(item);

        SaleDAO dao = new SaleDAO();
        assertThrows(SQLException.class, () -> dao.createSale(excessiveSale), "Excessive sale must throw SQLException due to insufficient stock");
    }

    @Test
    @Order(14)
    @DisplayName("14. Inventory Transactions Audit Log")
    void test14InventoryTransactionsLog() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM inventory_transactions WHERE business_id = ? AND product_id = ?")) {
            pstmt.setInt(1, businessId);
            pstmt.setInt(2, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 2, "Should have logged at least 2 inventory transactions (1 purchase, 1 sale)");
            }
        }
    }

    @Test
    @Order(15)
    @DisplayName("15. Payment Settlement Audit")
    void test15PaymentSettlement() throws Exception {
        // Create an unpaid sale
        Sale creditSale = new Sale();
        creditSale.setBusinessId(businessId);
        creditSale.setBranchId(branchId);
        creditSale.setUserId(userId);
        creditSale.setCustomerId(customerId);
        creditSale.setInvoiceNumber("INV-CREDIT-" + token);
        creditSale.setPaidAmount(BigDecimal.ZERO);
        creditSale.setPaymentMethod(PaymentMethod.CASH);

        SaleItem item = new SaleItem();
        item.setProductId(productId);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("150.00"));
        creditSale.getItems().add(item);

        SaleDAO saleDao = new SaleDAO();
        assertTrue(saleDao.createSale(creditSale));

        PaymentDAO paymentDao = new PaymentDAO();
        paymentDao.settleSale(businessId, creditSale.getId(), new BigDecimal("150.00"), PaymentMethod.CASH, "Full settlement");

        Sale settled = saleDao.findById(creditSale.getId());
        assertEquals(PaymentStatus.PAID, settled.getPaymentStatus(), "Payment status must be PAID");
    }

    @Test
    @Order(16)
    @DisplayName("16. Expense DAO Audit")
    void test16ExpenseCrud() throws Exception {
        Expense expense = new Expense();
        expense.setBusinessId(businessId);
        expense.setBranchId(branchId);
        expense.setUserId(userId);
        expense.setCategory(ExpenseCategory.UTILITIES);
        expense.setTitle("Electricity Bill");
        expense.setAmount(new BigDecimal("250.00"));
        expense.setExpenseDate(new Date(System.currentTimeMillis()));

        ExpenseDAO dao = new ExpenseDAO();
        int expId = dao.create(expense);
        assertTrue(expId > 0, "Expense ID should be > 0");

        List<Expense> list = dao.findByBusiness(businessId);
        assertFalse(list.isEmpty(), "Expense list should not be empty");
    }

    @Test
    @Order(17)
    @DisplayName("17. Report DAO Audit")
    void test17ReportQueries() throws Exception {
        ReportDAO reportDAO = new ReportDAO();
        ReportFilterDTO filter = new ReportFilterDTO();

        String[] reportTypes = {
                "Sales Report", "Purchase Report", "Inventory Valuation Report",
                "Expense Breakdown Report", "Profit / Loss Report", "Executive Summary"
        };

        for (String type : reportTypes) {
            filter.setReportType(type);
            List<Object[]> rows = reportDAO.generateReport(businessId, filter);
            assertNotNull(rows, "Report rows must not be null for " + type);
        }
    }

    @Test
    @Order(18)
    @DisplayName("18. Analytics DAO Audit")
    void test18AnalyticsQueries() throws Exception {
        AnalyticsDAO analyticsDAO = new AnalyticsDAO();
        DashboardMetricsDTO metrics = analyticsDAO.getDashboardMetrics(businessId, branchId, "Month", null, null);
        assertNotNull(metrics, "Dashboard metrics must not be null");
        assertNotNull(metrics.getTotalSales(), "Total sales must not be null");
        assertNotNull(metrics.getGrossProfit(), "Gross profit must not be null");
    }

    @Test
    @Order(19)
    @DisplayName("19. Rollback Safety Audit")
    void test19RollbackSafety() {
        Sale invalidSale = new Sale();
        invalidSale.setBusinessId(businessId);
        invalidSale.setBranchId(branchId);
        invalidSale.setUserId(userId);
        invalidSale.setCustomerId(customerId);
        invalidSale.setInvoiceNumber("INV-FAIL-" + token);

        SaleItem badItem = new SaleItem();
        badItem.setProductId(999999); // Invalid Product ID to force FK failure
        badItem.setQuantity(1);
        badItem.setUnitPrice(new BigDecimal("50.00"));
        invalidSale.getItems().add(badItem);

        SaleDAO dao = new SaleDAO();
        assertThrows(SQLException.class, () -> dao.createSale(invalidSale), "Invalid sale must rollback cleanly");
    }

    @Test
    @Order(20)
    @DisplayName("20. Persistence Across Session Restart Audit")
    void test20PersistenceAcrossRestart() throws Exception {
        UserSession.clearSession();
        AuthService auth = new AuthService();
        assertTrue(auth.login("audit_" + token, "Audit#Pass2026").isSuccess(), "Login after session clear must succeed");
        assertNotNull(new ProductDAO().findById(productId), "Product must still exist after restart");
    }
}
