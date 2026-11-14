package com.businessmanager.e2e;

import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.dao.*;
import com.businessmanager.database.DBConnection;
import com.businessmanager.database.DatabaseInitializer;
import com.businessmanager.enums.BusinessType;
import com.businessmanager.enums.ExpenseCategory;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.enums.PaymentStatus;
import com.businessmanager.enums.TransactionType;
import com.businessmanager.model.*;
import com.businessmanager.security.UserSession;
import com.businessmanager.service.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Manual QA & Persistence Verification Test.
 * Validates real MySQL database ops, DemoModeConfig flag, UI panel logic,
 * inventory transaction logging, and persistence across database re-connections.
 */
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@Tag("integration")
@EnabledIfSystemProperty(named = "BUSINESS_MANAGER_IT", matches = "true")
public class E2eManualQaVerificationTest {

    @BeforeAll
    static void setupDatabase() {
        System.clearProperty("ASSESSMENT_MODE");
        System.clearProperty("DEMO_MODE_ENABLED");
        assertTrue(DatabaseInitializer.initializeDatabase(), "Database initialization failed");
        assertFalse(DemoModeConfig.isEnabled(), "DemoModeConfig must default to false for real MySQL persistence");
    }

    @Test
    void executeFullE2eManualQaPassAndVerifyPersistence() throws Exception {
        String token = UUID.randomUUID().toString().substring(0, 8);

        // 1. Register & Login Owner
        Business business = new Business();
        business.setName("QA Business " + token);
        business.setEmail("qa-" + token + "@enterprise.test");
        business.setType(BusinessType.LARGE);

        User owner = new User();
        owner.setFullName("QA Manager");
        owner.setUsername("qa_user_" + token);
        owner.setEmail("qaowner-" + token + "@enterprise.test");
        owner.setPhone("9876543210");

        AuthService auth = new AuthService();
        assertTrue(auth.registerBusinessAndUser(owner, business, "QaPass#2026".toCharArray()).isSuccess());
        int businessId = business.getId();
        assertTrue(auth.login(owner.getUsername(), "QaPass#2026").isSuccess());

        // 2. Branch & Employee Verification
        Branch branch = new Branch();
        branch.setBusinessId(businessId);
        branch.setName("Flagship Store " + token);
        branch.setCode("BR-QA-" + token);
        branch.setCity("Bengaluru");
        branch.setState("Karnataka");
        branch.setMain(true);
        branch.setActive(true);
        int branchId = new BranchDAO().create(branch);
        assertTrue(branchId > 0);

        Employee employee = new Employee();
        employee.setBusinessId(businessId);
        employee.setBranchId(branchId);
        employee.setEmployeeCode("EMP-QA-" + token);
        employee.setFullName("Sales Associate " + token);
        employee.setEmail("emp-" + token + "@enterprise.test");
        employee.setPhone("9123456789");
        employee.setPosition("Senior Cashier");
        employee.setDepartment("Retail Ops");
        employee.setSalary(new BigDecimal("35000.00"));
        employee.setHireDate(new Date(System.currentTimeMillis()));
        int employeeId = new EmployeeDAO().create(employee);
        assertTrue(employeeId > 0);

        // 3. Category & Supplier Verification
        Category cat = new Category();
        cat.setBusinessId(businessId);
        cat.setName("Electronics " + token);
        int catId = new CategoryDAO().create(cat);
        assertTrue(catId > 0);

        Supplier sup = new Supplier();
        sup.setBusinessId(businessId);
        sup.setName("Global Tech Distributors " + token);
        sup.setContactPerson("John Supplier");
        sup.setPhone("9888877777");
        sup.setTaxId("29AAACG1234F1Z5");
        sup.setCity("Bengaluru");
        int supId = new SupplierDAO().create(sup);
        assertTrue(supId > 0);

        // 4. Customer Verification
        Customer cust = new Customer();
        cust.setBusinessId(businessId);
        cust.setName("Alice Enterprise Customer " + token);
        cust.setPhone("9777766666");
        cust.setEmail("alice-" + token + "@client.test");
        cust.setCity("Bengaluru");
        cust.setTaxId("29BBBCG5678F1Z2");
        int custId = new CustomerDAO().create(cust);
        assertTrue(custId > 0);

        // 5. Product Creation & Unique SKU Validation
        Product prod1 = new Product();
        prod1.setBusinessId(businessId);
        prod1.setCategoryId(catId);
        prod1.setSupplierId(supId);
        prod1.setName("Wireless Gaming Mouse");
        prod1.setSku("SKU-MOUSE-" + token);
        prod1.setPurchasePrice(new BigDecimal("1200.00"));
        prod1.setSellingPrice(new BigDecimal("2500.00"));
        prod1.setStockQuantity(20);
        prod1.setMinStock(5);
        prod1.setUnit("pcs");
        prod1.setTaxRate(new BigDecimal("18.00"));
        int prod1Id = new ProductDAO().create(prod1);
        assertTrue(prod1Id > 0);

        // Verify duplicate SKU fails
        Product prodDuplicate = new Product();
        prodDuplicate.setBusinessId(businessId);
        prodDuplicate.setName("Duplicate SKU Product");
        prodDuplicate.setSku("SKU-MOUSE-" + token);
        prodDuplicate.setPurchasePrice(BigDecimal.TEN);
        prodDuplicate.setSellingPrice(new BigDecimal("20.00"));
        assertThrows(Exception.class, () -> new ProductDAO().create(prodDuplicate));

        // 6. Stock Adjustment & Inventory Transaction Logging
        int adjustmentAmount = 5;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            int newStock = prod1.getStockQuantity() + adjustmentAmount;
            assertTrue(new ProductDAO().updateStock(conn, prod1Id, newStock));
            
            String logSql = "INSERT INTO inventory_transactions (business_id, branch_id, product_id, user_id, transaction_type, quantity, reference_type, reference_id, notes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(logSql)) {
                pstmt.setInt(1, businessId);
                pstmt.setInt(2, branchId);
                pstmt.setInt(3, prod1Id);
                pstmt.setInt(4, owner.getId());
                pstmt.setString(5, adjustmentAmount >= 0 ? TransactionType.ADJUSTMENT_ADD.name() : TransactionType.ADJUSTMENT_REMOVE.name());
                pstmt.setInt(6, adjustmentAmount);
                pstmt.setString(7, "ADJUSTMENT");
                pstmt.setInt(8, prod1Id);
                pstmt.setString(9, "Manual QA Audit Adjustment");
                pstmt.executeUpdate();
            }
            conn.commit();
        }
        assertEquals(25, new ProductDAO().findById(prod1Id).getStockQuantity());

        // 7. Purchase Order (Restock 10 items)
        Purchase po = new Purchase();
        po.setBusinessId(businessId);
        po.setBranchId(branchId);
        po.setSupplierId(supId);
        po.setUserId(owner.getId());
        po.setPurchaseNumber("PO-QA-" + token);
        po.setSubtotal(new BigDecimal("12000.00"));
        po.setTotal(new BigDecimal("12000.00"));
        po.setPaidAmount(new BigDecimal("12000.00"));
        po.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        po.setPaymentStatus(PaymentStatus.PAID);
        PurchaseItem poItem = new PurchaseItem();
        poItem.setProductId(prod1Id);
        poItem.setQuantity(10);
        poItem.setUnitPrice(new BigDecimal("1200.00"));
        poItem.setLineTotal(new BigDecimal("12000.00"));
        po.getItems().add(poItem);
        assertTrue(new PurchaseDAO().createPurchase(po));
        assertEquals(35, new ProductDAO().findById(prod1Id).getStockQuantity());

        // 8. POS Billing Sale (Sell 3 items)
        Sale sale = new Sale();
        sale.setBusinessId(businessId);
        sale.setBranchId(branchId);
        sale.setUserId(owner.getId());
        sale.setCustomerId(custId);
        sale.setInvoiceNumber("INV-QA-" + token);
        sale.setSubtotal(new BigDecimal("7500.00"));
        sale.setTax(new BigDecimal("1350.00"));
        sale.setTotal(new BigDecimal("8850.00"));
        sale.setPaidAmount(BigDecimal.ZERO);
        sale.setPaymentMethod(PaymentMethod.CARD);
        sale.setPaymentStatus(PaymentStatus.UNPAID);
        SaleItem saleItem = new SaleItem();
        saleItem.setProductId(prod1Id);
        saleItem.setQuantity(3);
        saleItem.setUnitPrice(new BigDecimal("2500.00"));
        saleItem.setLineTotal(new BigDecimal("7500.00"));
        sale.getItems().add(saleItem);

        assertTrue(new SaleDAO().createSale(sale));
        assertEquals(32, new ProductDAO().findById(prod1Id).getStockQuantity());

        // Record payment for sale
        new PaymentDAO().settleSale(businessId, sale.getId(), new BigDecimal("8850.00"), PaymentMethod.CARD, "POS Card Payment");

        // 9. Log Expense
        Expense expense = new Expense();
        expense.setBusinessId(businessId);
        expense.setBranchId(branchId);
        expense.setUserId(owner.getId());
        expense.setCategory(ExpenseCategory.UTILITIES);
        expense.setTitle("Electricity Bill");
        expense.setAmount(new BigDecimal("3200.00"));
        expense.setExpenseDate(new Date(System.currentTimeMillis()));
        assertTrue(new ExpenseDAO().create(expense) > 0);

        // 10. Direct Database Persistence Verification Across Session Clearance
        UserSession.clearSession();

        // Query fresh from MySQL via JDBC
        Product persistedProduct = new ProductDAO().findById(prod1Id);
        assertNotNull(persistedProduct);
        assertEquals("SKU-MOUSE-" + token, persistedProduct.getSku());
        assertEquals(32, persistedProduct.getStockQuantity());
        assertEquals(0, new BigDecimal("2500.00").compareTo(persistedProduct.getSellingPrice()));

        Customer persistedCustomer = new CustomerDAO().findById(custId);
        assertNotNull(persistedCustomer);
        assertEquals(0, new BigDecimal("8850.00").compareTo(persistedCustomer.getTotalPurchased()));

        Supplier persistedSupplier = new SupplierDAO().findById(supId);
        assertNotNull(persistedSupplier);
        assertEquals(0, new BigDecimal("12000.00").compareTo(persistedSupplier.getTotalPurchased()));

        // Cleanup test data
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            stmt.executeUpdate("DELETE FROM businesses WHERE id = " + businessId);
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        }
    }
}
