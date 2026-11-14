package com.businessmanager.service;

import com.businessmanager.dao.PurchaseDAO;
import com.businessmanager.model.Purchase;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class PurchaseService {

    private final PurchaseDAO purchaseDAO;

    public PurchaseService() {
        this.purchaseDAO = new PurchaseDAO();
    }

    public PurchaseService(PurchaseDAO purchaseDAO) {
        this.purchaseDAO = purchaseDAO;
    }

    public List<Purchase> getPurchasesForBusiness(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("PURCHASES")) {
            throw new SecurityException("Access denied to PURCHASES module.");
        }
        return purchaseDAO.findByBusiness(businessId);
    }

    public boolean createPurchase(Purchase purchase) throws SQLException {
        AccessControl.requireWrite("PURCHASES");
        return purchaseDAO.createPurchase(purchase);
    }
}
