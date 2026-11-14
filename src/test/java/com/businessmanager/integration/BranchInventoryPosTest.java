package com.businessmanager.integration;

import com.businessmanager.dao.*;
import com.businessmanager.database.DatabaseInitializer;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.enums.PaymentStatus;
import com.businessmanager.model.*;
import com.businessmanager.security.UserSession;
import com.businessmanager.service.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "BUSINESS_MANAGER_IT", matches = "true")
class BranchInventoryPosTest {

    private final ProductDAO productDAO = new ProductDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final BusinessDAO businessDAO = new BusinessDAO();

    @BeforeAll
    static void setupDatabase() {
        assertTrue(DatabaseInitializer.initializeDatabase(), "Database initialization failed");
    }

    @Test
    void testBranchInventoryCreationAndPosCheckout() throws Exception {
        AuthService authService = new AuthService();
        AuthService.AuthResult authResult = authService.login("admin@businessmanager.com", "admin123");
        assertTrue(authResult.isSuccess(), "Admin login failed");
        User admin = authResult.getUser();
        Business business = authResult.getBusiness();
        UserSession.startSession(admin, business);

        int businessId = admin.getBusinessId();
        Branch mainBranch = branchDAO.findMainBranch(businessId);
        assertNotNull(mainBranch, "Main branch must exist");
        int branchId = mainBranch.getId();

        // 1. Create a product with initial stock 10
        Product p = new Product();
        p.setBusinessId(businessId);
        p.setName("Branch POS Widget " + System.currentTimeMillis());
        p.setSku("BPW-" + System.currentTimeMillis());
        p.setPurchasePrice(new BigDecimal("150.00"));
        p.setSellingPrice(new BigDecimal("300.00"));
        p.setStockQuantity(10);
        p.setMinStock(2);
        p.setActive(true);

        int productId = productDAO.create(p);
        assertTrue(productId > 0, "Product creation failed");

        // 2. Verify findByBusinessAndBranch loads stock 10 for main branch
        List<Product> branchProducts = productDAO.findByBusinessAndBranch(businessId, branchId);
        Product loadedProduct = branchProducts.stream().filter(prod -> prod.getId() == productId).findFirst().orElse(null);
        assertNotNull(loadedProduct, "Product must be returned by findByBusinessAndBranch");
        assertEquals(10, loadedProduct.getStockQuantity(), "Branch inventory quantity must equal 10");

        // 3. Perform a sale of 2 units
        Sale sale = new Sale();
        sale.setBusinessId(businessId);
        sale.setBranchId(branchId);
        sale.setUserId(admin.getId());
        sale.setInvoiceNumber("INV-BRANCH-" + System.currentTimeMillis());
        sale.setSubtotal(new BigDecimal("600.00"));
        sale.setTax(BigDecimal.ZERO);
        sale.setDiscount(BigDecimal.ZERO);
        sale.setTotal(new BigDecimal("600.00"));
        sale.setPaidAmount(new BigDecimal("600.00"));
        sale.setPaymentMethod(PaymentMethod.CASH);
        sale.setPaymentStatus(PaymentStatus.PAID);

        SaleItem item = new SaleItem();
        item.setProductId(productId);
        item.setProductName(p.getName());
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("300.00"));
        item.setLineTotal(new BigDecimal("600.00"));
        sale.getItems().add(item);

        assertTrue(saleDAO.createSale(sale), "Sale execution failed");

        // 4. Verify branch stock decreased to 8
        List<Product> postSaleProducts = productDAO.findByBusinessAndBranch(businessId, branchId);
        Product postSaleProduct = postSaleProducts.stream().filter(prod -> prod.getId() == productId).findFirst().orElse(null);
        assertNotNull(postSaleProduct);
        assertEquals(8, postSaleProduct.getStockQuantity(), "Branch stock should decrease to 8 after selling 2 units");

        // 5. Test Negative Stock rejection (attempt to sell 20 units when stock is 8)
        Sale invalidSale = new Sale();
        invalidSale.setBusinessId(businessId);
        invalidSale.setBranchId(branchId);
        invalidSale.setUserId(admin.getId());
        invalidSale.setInvoiceNumber("INV-EXCEED-" + System.currentTimeMillis());
        invalidSale.setSubtotal(new BigDecimal("6000.00"));
        invalidSale.setTotal(new BigDecimal("6000.00"));
        invalidSale.setPaidAmount(new BigDecimal("6000.00"));
        invalidSale.setPaymentMethod(PaymentMethod.CASH);
        invalidSale.setPaymentStatus(PaymentStatus.PAID);

        SaleItem exceedItem = new SaleItem();
        exceedItem.setProductId(productId);
        exceedItem.setProductName(p.getName());
        exceedItem.setQuantity(20);
        exceedItem.setUnitPrice(new BigDecimal("300.00"));
        exceedItem.setLineTotal(new BigDecimal("6000.00"));
        invalidSale.getItems().add(exceedItem);

        SQLException ex = assertThrows(SQLException.class, () -> saleDAO.createSale(invalidSale));
        assertTrue(ex.getMessage().contains("Insufficient stock for " + p.getName()), "Exception message should contain product name and available quantity");

        // Clean up test records
        try (var conn = com.businessmanager.database.DBConnection.getConnection()) {
            try (var stmt = conn.prepareStatement("DELETE FROM sale_items WHERE product_id = ?")) {
                stmt.setInt(1, productId);
                stmt.executeUpdate();
            }
            try (var stmt = conn.prepareStatement("DELETE FROM inventory_transactions WHERE product_id = ?")) {
                stmt.setInt(1, productId);
                stmt.executeUpdate();
            }
            if (sale.getId() > 0) {
                try (var stmt = conn.prepareStatement("DELETE FROM sales WHERE id = ?")) {
                    stmt.setInt(1, sale.getId());
                    stmt.executeUpdate();
                }
            }
            try (var stmt = conn.prepareStatement("DELETE FROM branch_inventory WHERE product_id = ?")) {
                stmt.setInt(1, productId);
                stmt.executeUpdate();
            }
        }
        productDAO.delete(productId);
    }
}
