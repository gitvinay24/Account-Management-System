package com.businessmanager.integration;

import com.businessmanager.dao.BranchDAO;
import com.businessmanager.dao.CategoryDAO;
import com.businessmanager.dao.CustomerDAO;
import com.businessmanager.dao.EmployeeDAO;
import com.businessmanager.dao.ExpenseDAO;
import com.businessmanager.dao.PaymentDAO;
import com.businessmanager.dao.ProductDAO;
import com.businessmanager.dao.PurchaseDAO;
import com.businessmanager.dao.SaleDAO;
import com.businessmanager.dao.SupplierDAO;
import com.businessmanager.database.DBConnection;
import com.businessmanager.database.DatabaseInitializer;
import com.businessmanager.enums.BusinessType;
import com.businessmanager.enums.ExpenseCategory;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.enums.PaymentStatus;
import com.businessmanager.model.Branch;
import com.businessmanager.model.Business;
import com.businessmanager.model.Category;
import com.businessmanager.model.Customer;
import com.businessmanager.model.Employee;
import com.businessmanager.model.Expense;
import com.businessmanager.model.Product;
import com.businessmanager.model.Purchase;
import com.businessmanager.model.PurchaseItem;
import com.businessmanager.model.Sale;
import com.businessmanager.model.SaleItem;
import com.businessmanager.model.Supplier;
import com.businessmanager.model.User;
import com.businessmanager.security.UserSession;
import com.businessmanager.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL smoke coverage. Set BUSINESS_MANAGER_IT=true only against a disposable/test database. */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "BUSINESS_MANAGER_IT", matches = "true")
class MySqlIntegrationTest {
    private Integer businessId;

    @BeforeAll
    static void setupDatabase() {
        assertTrue(DatabaseInitializer.initializeDatabase(), "Database initialization failed");
    }

    @AfterEach
    void cleanup() throws Exception {
        UserSession.clearSession();
        if (businessId == null) return;
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // purchase_items and sale_items also reference products, so remove
                // only the child rows owned by this uniquely-created test business
                // before the business-level cascade removes the parent records.
                deleteForBusiness(connection,
                        "DELETE pi FROM purchase_items pi JOIN purchases p ON p.id = pi.purchase_id WHERE p.business_id = ?");
                deleteForBusiness(connection,
                        "DELETE si FROM sale_items si JOIN sales s ON s.id = si.sale_id WHERE s.business_id = ?");
                deleteForBusiness(connection, "DELETE FROM businesses WHERE id = ?");
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void deleteForBusiness(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, businessId);
            statement.executeUpdate();
        }
    }

    @Test
    void authenticatesCrudAndSettlesInventorySaleAgainstMysql() throws Exception {
        String token = UUID.randomUUID().toString().substring(0, 8);
        Business business = new Business();
        business.setName("Integration " + token);
        business.setEmail("it-" + token + "@example.test");
        business.setType(BusinessType.LARGE);
        
        User owner = new User();
        owner.setFullName("Integration Owner");
        owner.setUsername("it" + token);
        owner.setEmail("owner-" + token + "@example.test");
        owner.setPhone("9999999999");
        
        AuthService auth = new AuthService();
        assertTrue(auth.registerBusinessAndUser(owner, business, "Strong#Password1".toCharArray()).isSuccess());
        businessId = business.getId();
        assertTrue(auth.login(owner.getUsername(), "Strong#Password1").isSuccess());

        // 1. Branch Creation
        Branch branch = new Branch();
        branch.setBusinessId(businessId);
        branch.setName("Main Branch " + token);
        branch.setCode("BR-" + token);
        branch.setCity("Mumbai");
        branch.setMain(true);
        int branchId = new BranchDAO().create(branch);
        assertTrue(branchId > 0);

        // 2. Category Creation
        Category category = new Category();
        category.setBusinessId(businessId);
        category.setName("IT Category " + token);
        int categoryId = new CategoryDAO().create(category);
        assertTrue(categoryId > 0);

        // 3. Supplier & Customer Creation
        Supplier supplier = new Supplier();
        supplier.setBusinessId(businessId);
        supplier.setName("IT Supplier " + token);
        supplier.setEmail("sup-" + token + "@example.test");
        int supplierId = new SupplierDAO().create(supplier);
        assertTrue(supplierId > 0);

        Customer customer = new Customer();
        customer.setBusinessId(businessId);
        customer.setName("IT Customer " + token);
        customer.setEmail("cust-" + token + "@example.test");
        int customerId = new CustomerDAO().create(customer);
        assertTrue(customerId > 0);

        // 4. Product Creation
        Product product = new Product();
        product.setBusinessId(businessId);
        product.setCategoryId(categoryId);
        product.setSupplierId(supplierId);
        product.setName("IT Product");
        product.setSku("IT-" + token);
        product.setPurchasePrice(BigDecimal.TEN);
        product.setSellingPrice(new BigDecimal("20.00"));
        product.setStockQuantity(5);
        product.setMinStock(1);
        product.setUnit("pcs");
        product.setTaxRate(BigDecimal.ZERO);
        int productId = new ProductDAO().create(product);
        assertTrue(productId > 0);

        // 5. Purchase Transaction (Restock)
        Purchase purchase = new Purchase();
        purchase.setBusinessId(businessId);
        purchase.setBranchId(branchId);
        purchase.setSupplierId(supplierId);
        purchase.setUserId(owner.getId());
        purchase.setPurchaseNumber("PO-" + token);
        purchase.setSubtotal(new BigDecimal("100.00"));
        purchase.setTotal(new BigDecimal("100.00"));
        purchase.setPaidAmount(new BigDecimal("100.00"));
        purchase.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        purchase.setPaymentStatus(PaymentStatus.PAID);
        PurchaseItem pItem = new PurchaseItem();
        pItem.setProductId(productId);
        pItem.setQuantity(10);
        pItem.setUnitPrice(BigDecimal.TEN);
        pItem.setLineTotal(new BigDecimal("100.00"));
        purchase.getItems().add(pItem);
        assertTrue(new PurchaseDAO().createPurchase(purchase));
        assertEquals(15, new ProductDAO().findById(productId).getStockQuantity());

        // 6. POS Sale Transaction
        Sale sale = new Sale();
        sale.setBusinessId(businessId);
        sale.setBranchId(branchId);
        sale.setUserId(owner.getId());
        sale.setCustomerId(customerId);
        sale.setInvoiceNumber("IT-INV-" + token);
        sale.setSubtotal(new BigDecimal("40.00"));
        sale.setTotal(new BigDecimal("40.00"));
        sale.setPaidAmount(BigDecimal.ZERO);
        sale.setPaymentMethod(PaymentMethod.UPI);
        sale.setPaymentStatus(PaymentStatus.UNPAID);
        SaleItem sItem = new SaleItem();
        sItem.setProductId(productId);
        sItem.setQuantity(2);
        sItem.setUnitPrice(new BigDecimal("20.00"));
        sItem.setLineTotal(new BigDecimal("40.00"));
        sale.getItems().add(sItem);
        assertTrue(new SaleDAO().createSale(sale));
        assertEquals(13, new ProductDAO().findById(productId).getStockQuantity());

        // 7. Payment Settlement
        new PaymentDAO().settleSale(businessId, sale.getId(), new BigDecimal("40.00"), PaymentMethod.UPI, "Integration payment");
        assertEquals(PaymentStatus.PAID, new SaleDAO().findById(sale.getId()).getPaymentStatus());

        // 7b. Tender greater than the total must save the sale/payment at the actual total,
        // persist the change separately, and leave invoice data attached to the committed sale.
        Sale cashSale = new Sale();
        cashSale.setBusinessId(businessId);
        cashSale.setBranchId(branchId);
        cashSale.setUserId(owner.getId());
        cashSale.setCustomerId(customerId);
        cashSale.setInvoiceNumber("IT-CASH-" + token);
        cashSale.setPaidAmount(new BigDecimal("25.00"));
        cashSale.setPaymentMethod(PaymentMethod.CASH);
        SaleItem cashItem = new SaleItem();
        cashItem.setProductId(productId);
        cashItem.setQuantity(1);
        cashItem.setUnitPrice(new BigDecimal("20.00"));
        cashSale.getItems().add(cashItem);
        assertTrue(new SaleDAO().createSale(cashSale));
        Sale committedCashSale = new SaleDAO().findById(cashSale.getId());
        assertEquals(0, committedCashSale.getTotal().compareTo(new BigDecimal("20.00")));
        assertEquals(0, committedCashSale.getPaidAmount().compareTo(new BigDecimal("20.00")));
        assertEquals(0, committedCashSale.getChangeAmount().compareTo(new BigDecimal("5.00")));
        assertEquals(1, committedCashSale.getItems().size());
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement payment = connection.prepareStatement("SELECT amount FROM payments WHERE reference_type = 'SALE' AND reference_id = ?")) {
            payment.setInt(1, cashSale.getId());
            try (var rows = payment.executeQuery()) {
                assertTrue(rows.next(), "Committed customer sale must create its payment entry");
                assertEquals(0, rows.getBigDecimal("amount").compareTo(new BigDecimal("20.00")));
            }
        }

        // 8. Expense Creation
        Expense expense = new Expense();
        expense.setBusinessId(businessId);
        expense.setBranchId(branchId);
        expense.setUserId(owner.getId());
        expense.setCategory(ExpenseCategory.RENT);
        expense.setTitle("Monthly Store Rent");
        expense.setAmount(new BigDecimal("500.00"));
        expense.setExpenseDate(new Date(System.currentTimeMillis()));
        assertTrue(new ExpenseDAO().create(expense) > 0);
    }
}
