package com.businessmanager.demo;

import com.businessmanager.dao.*;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.model.*;
import com.businessmanager.service.DemoAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DemoAssessmentFlowTest {
    @BeforeEach void setUp() {
        System.setProperty("ASSESSMENT_MODE", "true");
        DemoDataStore.reset();
        assertNotNull(new DemoAuthenticationService().authenticate("admin@businessmanager.com", "admin123"));
    }

    @org.junit.jupiter.api.AfterEach void tearDown() {
        System.clearProperty("ASSESSMENT_MODE");
    }

    @Test void crudAndTransactionsUseLiveOfflineData() throws Exception {
        ProductDAO products = new ProductDAO();
        Category category = new Category(); category.setName("Demo Test Category");
        int categoryId = new CategoryDAO().create(category);
        assertTrue(categoryId > 0);

        Product product = new Product(); product.setName("Demo Test Product"); product.setSku("DEMO-TEST-01");
        product.setCategoryId(categoryId); product.setPurchasePrice(new BigDecimal("10")); product.setSellingPrice(new BigDecimal("20")); product.setStockQuantity(5);
        int productId = products.create(product);
        assertEquals(5, products.findById(productId).getStockQuantity());

        Purchase purchase = new Purchase(); purchase.setSupplierId(DemoDataStore.suppliers(-1).get(0).getId()); purchase.setPurchaseNumber("PO-TEST"); purchase.setPaidAmount(BigDecimal.ZERO);
        PurchaseItem purchaseItem = new PurchaseItem(); purchaseItem.setProductId(productId); purchaseItem.setQuantity(10); purchaseItem.setUnitPrice(new BigDecimal("10")); purchaseItem.setLineTotal(new BigDecimal("100")); purchase.getItems().add(purchaseItem);
        assertTrue(new PurchaseDAO().createPurchase(purchase));
        assertEquals(15, products.findById(productId).getStockQuantity());

        Sale sale = new Sale(); sale.setInvoiceNumber("INV-TEST"); sale.setCustomerId(DemoDataStore.customers(-1).get(0).getId()); sale.setPaidAmount(new BigDecimal("40"));
        SaleItem saleItem = new SaleItem(); saleItem.setProductId(productId); saleItem.setProductName(product.getName()); saleItem.setQuantity(2); saleItem.setUnitPrice(new BigDecimal("20")); saleItem.setLineTotal(new BigDecimal("40")); sale.getItems().add(saleItem);
        assertTrue(new SaleDAO().createSale(sale));
        assertEquals(13, products.findById(productId).getStockQuantity());
        assertFalse(DemoDataStore.reportRows(new ReportFilterDTO()).isEmpty());

        assertThrows(Exception.class, () -> new PaymentDAO().settleSale(-1, sale.getId(), new BigDecimal("1"), PaymentMethod.CASH, "overpayment"));
    }
}
