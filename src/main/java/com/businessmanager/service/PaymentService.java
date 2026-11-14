package com.businessmanager.service;

import com.businessmanager.dao.PaymentDAO;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.model.Purchase;
import com.businessmanager.model.Sale;
import com.businessmanager.security.AccessControl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class PaymentService {

    private final PaymentDAO paymentDAO;

    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
    }

    public PaymentService(PaymentDAO paymentDAO) {
        this.paymentDAO = paymentDAO;
    }

    public List<Sale> getOutstandingSales(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("PAYMENTS")) {
            throw new SecurityException("Access denied to PAYMENTS module.");
        }
        return paymentDAO.findOutstandingSales(businessId);
    }

    public List<Purchase> getOutstandingPurchases(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("PAYMENTS")) {
            throw new SecurityException("Access denied to PAYMENTS module.");
        }
        return paymentDAO.findOutstandingPurchases(businessId);
    }

    public void settleSale(int businessId, int saleId, BigDecimal amount, PaymentMethod method, String notes) throws SQLException {
        AccessControl.requireWrite("PAYMENTS");
        paymentDAO.settleSale(businessId, saleId, amount, method, notes);
    }

    public void settlePurchase(int businessId, int purchaseId, BigDecimal amount, PaymentMethod method, String notes) throws SQLException {
        AccessControl.requireWrite("PAYMENTS");
        paymentDAO.settlePurchase(businessId, purchaseId, amount, method, notes);
    }
}
