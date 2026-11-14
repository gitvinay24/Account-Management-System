package com.businessmanager.e2e;

import com.businessmanager.dao.AnalyticsDAO;
import com.businessmanager.dao.BranchDAO;
import com.businessmanager.dao.BusinessDAO;
import com.businessmanager.database.DatabaseInitializer;
import com.businessmanager.model.Coupon;
import com.businessmanager.model.DashboardMetricsDTO;
import com.businessmanager.model.PosCalculationResult;
import com.businessmanager.model.SaleItem;
import com.businessmanager.service.CouponService;
import com.businessmanager.service.PosCalculationEngine;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StitchDashboardPosVerificationTest {

    private static AnalyticsDAO analyticsDAO;
    private static BranchDAO branchDAO;
    private static BusinessDAO businessDAO;
    private static PosCalculationEngine calculationEngine;
    private static CouponService couponService;

    @BeforeAll
    static void setUpAll() {
        DatabaseInitializer.initializeDatabase();
        analyticsDAO = new AnalyticsDAO();
        branchDAO = new BranchDAO();
        businessDAO = new BusinessDAO();
        calculationEngine = new PosCalculationEngine();
        couponService = new CouponService();
    }

    @Test
    void testDashboardDataAggregation() throws Exception {
        DashboardMetricsDTO metrics = analyticsDAO.getDashboardMetrics(1, null, "Today", null, null);
        assertNotNull(metrics, "Metrics DTO must not be null");

        DefaultCategoryDataset trendDs = analyticsDAO.getSalesVsPurchasesTrendDataset(1, null, "Month", null, null);
        assertNotNull(trendDs, "Trend dataset must not be null");

        var topProducts = analyticsDAO.getTopSellingProductsList(1, null, "Month", null, null, 5);
        assertNotNull(topProducts, "Top products list must not be null");

        var lowStock = analyticsDAO.getLowStockProducts(1, null);
        assertNotNull(lowStock, "Low stock list must not be null");
    }

    @Test
    void testPosCalculationEnginePrecisionAndGst() {
        List<SaleItem> items = new ArrayList<>();
        SaleItem item = new SaleItem();
        item.setUnitPrice(new BigDecimal("10000.00"));
        item.setQuantity(1);
        item.setDiscount(BigDecimal.ZERO);
        items.add(item);

        Coupon welcome10 = couponService.findCoupon("WELCOME10");
        assertNotNull(welcome10);

        PosCalculationResult result = calculationEngine.calculate(
                items,
                "WELCOME10",
                true,
                new BigDecimal("18.00"),
                new BigDecimal("12000.00")
        );

        // Subtotal: 10,000 | Coupon 10%: 1,000 | Taxable: 9,000 | GST 18%: 1,620 | Grand Total: 10,620 | Change: 1,380
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.getSubtotal()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(result.getCouponDiscount()));
        assertEquals(0, new BigDecimal("9000.00").compareTo(result.getTaxableAmount()));
        assertEquals(0, new BigDecimal("1620.00").compareTo(result.getGstAmount()));
        assertEquals(0, new BigDecimal("10620.00").compareTo(result.getGrandTotal()));
        assertEquals(0, new BigDecimal("1380.00").compareTo(result.getChangeAmount()));
    }

    @Test
    void testBranchDataFetch() throws Exception {
        var branches = branchDAO.findByBusiness(1);
        assertNotNull(branches);
    }
}
