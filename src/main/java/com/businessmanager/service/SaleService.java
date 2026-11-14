package com.businessmanager.service;

import com.businessmanager.dao.SaleDAO;
import com.businessmanager.model.Sale;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class SaleService {

    private final SaleDAO saleDAO;

    public SaleService() {
        this.saleDAO = new SaleDAO();
    }

    public SaleService(SaleDAO saleDAO) {
        this.saleDAO = saleDAO;
    }

    public List<Sale> getSalesForBusiness(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("SALES")) {
            throw new SecurityException("Access denied to SALES module.");
        }
        return saleDAO.findByBusiness(businessId);
    }

    public Sale getSaleById(int id) throws SQLException {
        if (!AccessControl.canAccessModule("SALES") && !AccessControl.canAccessModule("POS")) {
            throw new SecurityException("Access denied to SALES module.");
        }
        return saleDAO.findById(id);
    }

    public boolean createSale(Sale sale) throws SQLException {
        AccessControl.requireWrite("POS");
        return saleDAO.createSale(sale);
    }
}
