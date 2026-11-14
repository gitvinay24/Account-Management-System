package com.businessmanager.service;

import com.businessmanager.dao.SupplierDAO;
import com.businessmanager.model.Supplier;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class SupplierService {

    private final SupplierDAO supplierDAO;

    public SupplierService() {
        this.supplierDAO = new SupplierDAO();
    }

    public SupplierService(SupplierDAO supplierDAO) {
        this.supplierDAO = supplierDAO;
    }

    public List<Supplier> getSuppliersForBusiness(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("SUPPLIERS")) {
            throw new SecurityException("Access denied to SUPPLIERS module.");
        }
        return supplierDAO.findByBusiness(businessId);
    }

    public Supplier getSupplierById(int id) throws SQLException {
        if (!AccessControl.canAccessModule("SUPPLIERS")) {
            throw new SecurityException("Access denied to SUPPLIERS module.");
        }
        return supplierDAO.findById(id);
    }

    public int createSupplier(Supplier supplier) throws SQLException {
        AccessControl.requireWrite("SUPPLIERS");
        return supplierDAO.create(supplier);
    }

    public boolean updateSupplier(Supplier supplier) throws SQLException {
        AccessControl.requireWrite("SUPPLIERS");
        return supplierDAO.update(supplier);
    }

    public boolean deleteSupplier(int id) throws SQLException {
        AccessControl.requireWrite("SUPPLIERS");
        return supplierDAO.delete(id);
    }
}
