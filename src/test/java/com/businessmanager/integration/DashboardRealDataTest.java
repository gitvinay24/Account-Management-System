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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "BUSINESS_MANAGER_IT", matches = "true")
class DashboardRealDataTest {

    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final BusinessDAO businessDAO = new BusinessDAO();

    @BeforeAll
    static void setupDatabase() {
        assertTrue(DatabaseInitializer.initializeDatabase(), "Database initialization failed");
    }

    @Test
    void testDashboardRealDataMetricsAndFiltering() throws Exception {
        AuthService authService = new AuthService();
        AuthService.AuthResult authResult = authService.login("admin@businessmanager.com", "admin123");
        assertTrue(authResult.isSuccess(), "Admin login failed: " + authResult.getMessage());
        User admin = authResult.getUser();
        Business business = authResult.getBusiness();
        assertNotNull(admin, "Admin user should not be null");
        assertNotNull(business, "Business should not be null");
        UserSession.startSession(admin, business);

        int businessId = admin.getBusinessId();

        // 1. Query initial metrics
        DashboardMetricsDTO metrics = analyticsDAO.getDashboardMetrics(businessId, null, "Month", null, null);
        assertNotNull(metrics, "Metrics DTO should not be null");
        assertNotNull(metrics.getTotalSales(), "Total sales should not be null");
        assertNotNull(metrics.getGrossProfit(), "Gross profit should not be null");
        assertNotNull(metrics.getTotalPurchases(), "Total purchases should not be null");
        assertNotNull(metrics.getInventoryValue(), "Inventory value should not be null");

        // 2. Query top products list
        List<TopProductDTO> topProducts = analyticsDAO.getTopSellingProductsList(businessId, null, "Month", null, null, 5);
        assertNotNull(topProducts, "Top products list should not be null");

        // 3. Query low stock products
        List<LowStockProductDTO> lowStock = analyticsDAO.getLowStockProducts(businessId, null);
        assertNotNull(lowStock, "Low stock products should not be null");

        // 4. Query recent sales list
        List<RecentSaleDTO> recentSales = analyticsDAO.getRecentSalesList(businessId, null, 5);
        assertNotNull(recentSales, "Recent sales list should not be null");

        // 5. Query recent activities
        List<RecentActivityDTO> activities = analyticsDAO.getRecentActivitiesList(businessId, null, 5);
        assertNotNull(activities, "Recent activities should not be null");
    }

    @Test
    void testSaleTransactionReflectsOnDashboard() throws Exception {
        AuthService authService = new AuthService();
        AuthService.AuthResult authResult = authService.login("admin@businessmanager.com", "admin123");
        assertTrue(authResult.isSuccess(), "Admin login failed: " + authResult.getMessage());
        User admin = authResult.getUser();
        Business business = authResult.getBusiness();
        assertNotNull(admin);
        assertNotNull(business);
        UserSession.startSession(admin, business);

        int businessId = admin.getBusinessId();

        DashboardMetricsDTO initialMetrics = analyticsDAO.getDashboardMetrics(businessId, null, "Today", null, null);
        BigDecimal initialSales = initialMetrics.getTotalSales();

        // Create a product
        Product p = new Product();
        p.setBusinessId(businessId);
        p.setName("Dashboard Test Widget " + System.currentTimeMillis());
        p.setSku("DTW-" + System.currentTimeMillis());
        p.setPurchasePrice(new BigDecimal("100.00"));
        p.setSellingPrice(new BigDecimal("250.00"));
        p.setStockQuantity(50);
        p.setMinStock(5);
        p.setActive(true);
        int newProdId = productDAO.create(p);
        assertTrue(newProdId > 0, "Creating product failed");

        // Perform a sale
        Sale sale = new Sale();
        sale.setBusinessId(businessId);
        sale.setUserId(admin.getId());
        sale.setInvoiceNumber("INV-" + System.currentTimeMillis());
        sale.setSubtotal(new BigDecimal("250.00"));
        sale.setTax(BigDecimal.ZERO);
        sale.setDiscount(BigDecimal.ZERO);
        sale.setTotal(new BigDecimal("250.00"));
        sale.setPaidAmount(new BigDecimal("250.00"));
        sale.setPaymentMethod(PaymentMethod.CASH);
        sale.setPaymentStatus(PaymentStatus.PAID);

        SaleItem item = new SaleItem();
        item.setProductId(newProdId);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("250.00"));
        item.setLineTotal(new BigDecimal("250.00"));

        List<SaleItem> items = new ArrayList<>();
        items.add(item);
        sale.setItems(items);

        assertTrue(saleDAO.createSale(sale), "Creating sale failed");

        // Verify updated metrics
        DashboardMetricsDTO updatedMetrics = analyticsDAO.getDashboardMetrics(businessId, null, "Today", null, null);
        assertTrue(updatedMetrics.getTotalSales().compareTo(initialSales) > 0, "Dashboard total sales should increase after new sale");
    }
}
