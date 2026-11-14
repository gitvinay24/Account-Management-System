package com.businessmanager.unit;

import com.businessmanager.model.PosCalculationResult;
import com.businessmanager.model.SaleItem;
import com.businessmanager.service.PosCalculationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PosBillingEngineTest {

    private PosCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PosCalculationEngine();
    }

    @Test
    @DisplayName("Calculate simple cart subtotal without discount or GST")
    void testSimpleSubtotal() {
        List<SaleItem> items = new ArrayList<>();
        SaleItem item1 = new SaleItem();
        item1.setUnitPrice(new BigDecimal("70000.00"));
        item1.setQuantity(1);
        items.add(item1);

        SaleItem item2 = new SaleItem();
        item2.setUnitPrice(new BigDecimal("1000.00"));
        item2.setQuantity(1);
        items.add(item2);

        PosCalculationResult res = engine.calculate(items, null, false, BigDecimal.ZERO, new BigDecimal("71000.00"));

        assertEquals(0, new BigDecimal("71000.00").compareTo(res.getSubtotal()));
        assertEquals(0, new BigDecimal("0.00").compareTo(res.getTotalDiscount()));
        assertEquals(0, new BigDecimal("71000.00").compareTo(res.getTaxableAmount()));
        assertEquals(0, new BigDecimal("0.00").compareTo(res.getGstAmount()));
        assertEquals(0, new BigDecimal("71000.00").compareTo(res.getGrandTotal()));
        assertEquals(0, new BigDecimal("0.00").compareTo(res.getChangeAmount()));
        assertTrue(res.isPaymentSufficient());
    }

    @Test
    @DisplayName("Calculate GST 18% on ₹10,000 subtotal")
    void testGstCalculation() {
        List<SaleItem> items = new ArrayList<>();
        SaleItem item = new SaleItem();
        item.setUnitPrice(new BigDecimal("10000.00"));
        item.setQuantity(1);
        items.add(item);

        PosCalculationResult res = engine.calculate(items, null, true, new BigDecimal("18.00"), new BigDecimal("12000.00"));

        assertEquals(0, new BigDecimal("10000.00").compareTo(res.getSubtotal()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(res.getTaxableAmount()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(res.getGstAmount()));
        assertEquals(0, new BigDecimal("11800.00").compareTo(res.getGrandTotal()));
        assertEquals(0, new BigDecimal("200.00").compareTo(res.getChangeAmount()));
        assertTrue(res.isPaymentSufficient());
    }

    @Test
    @DisplayName("Apply WELCOME10 coupon (10% off) and GST 18%")
    void testCouponWelcome10AndGst() {
        List<SaleItem> items = new ArrayList<>();
        SaleItem item = new SaleItem();
        item.setUnitPrice(new BigDecimal("10000.00"));
        item.setQuantity(1);
        items.add(item);

        // Subtotal = 10000, 10% coupon = 1000, Taxable = 9000, GST 18% of 9000 = 1620, Grand Total = 10620
        PosCalculationResult res = engine.calculate(items, "WELCOME10", true, new BigDecimal("18.00"), new BigDecimal("11000.00"));

        assertEquals(0, new BigDecimal("10000.00").compareTo(res.getSubtotal()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(res.getCouponDiscount()));
        assertEquals(0, new BigDecimal("9000.00").compareTo(res.getTaxableAmount()));
        assertEquals(0, new BigDecimal("1620.00").compareTo(res.getGstAmount()));
        assertEquals(0, new BigDecimal("10620.00").compareTo(res.getGrandTotal()));
        assertEquals(0, new BigDecimal("380.00").compareTo(res.getChangeAmount()));
        assertTrue(res.isPaymentSufficient());
    }

    @Test
    @DisplayName("Apply FLAT500 coupon (₹500 off) with minimum purchase rule")
    void testCouponFlat500() {
        List<SaleItem> items = new ArrayList<>();
        SaleItem item = new SaleItem();
        item.setUnitPrice(new BigDecimal("2500.00"));
        item.setQuantity(1);
        items.add(item);

        // Subtotal = 2500, FLAT500 coupon = 500, Taxable = 2000, GST 0%
        PosCalculationResult res = engine.calculate(items, "FLAT500", false, BigDecimal.ZERO, new BigDecimal("2000.00"));

        assertEquals(0, new BigDecimal("2500.00").compareTo(res.getSubtotal()));
        assertEquals(0, new BigDecimal("500.00").compareTo(res.getCouponDiscount()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(res.getGrandTotal()));
        assertTrue(res.isPaymentSufficient());
    }

    @Test
    @DisplayName("Reject FLAT500 if subtotal below minimum purchase threshold")
    void testCouponFlat500MinimumPurchaseRequirement() {
        List<SaleItem> items = new ArrayList<>();
        SaleItem item = new SaleItem();
        item.setUnitPrice(new BigDecimal("1500.00"));
        item.setQuantity(1);
        items.add(item);

        // Subtotal = 1500 < 2000 min purchase threshold -> coupon discount = 0
        PosCalculationResult res = engine.calculate(items, "FLAT500", false, BigDecimal.ZERO, new BigDecimal("1500.00"));

        assertEquals(0, new BigDecimal("1500.00").compareTo(res.getSubtotal()));
        assertEquals(0, new BigDecimal("0.00").compareTo(res.getCouponDiscount()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(res.getGrandTotal()));
    }

    @Test
    @DisplayName("Detect insufficient payment")
    void testUnderpaymentDetection() {
        List<SaleItem> items = new ArrayList<>();
        SaleItem item = new SaleItem();
        item.setUnitPrice(new BigDecimal("10000.00"));
        item.setQuantity(1);
        items.add(item);

        // Grand Total = 10000, Payment Received = 9000 -> insufficient
        PosCalculationResult res = engine.calculate(items, null, false, BigDecimal.ZERO, new BigDecimal("9000.00"));

        assertEquals(0, new BigDecimal("10000.00").compareTo(res.getGrandTotal()));
        assertFalse(res.isPaymentSufficient());
    }
}
