package com.businessmanager.service;

import com.businessmanager.dao.ProductDAO;
import com.businessmanager.model.Product;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class ProductService {

    private final ProductDAO productDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
    }

    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public List<Product> getProductsForBusiness(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("PRODUCTS")) {
            throw new SecurityException("Access denied to PRODUCTS module.");
        }
        return productDAO.findByBusiness(businessId);
    }

    public List<Product> getProductsForBranch(int businessId, int branchId) throws SQLException {
        if (!AccessControl.canAccessModule("PRODUCTS") && !AccessControl.canAccessModule("POS")) {
            throw new SecurityException("Access denied to PRODUCTS / POS module.");
        }
        return productDAO.findByBusinessAndBranch(businessId, branchId);
    }

    public List<Product> getLowStockProducts(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("PRODUCTS") && !AccessControl.canAccessModule("INVENTORY")) {
            throw new SecurityException("Access denied to INVENTORY module.");
        }
        return productDAO.findLowStock(businessId);
    }

    public Product getProductById(int id) throws SQLException {
        if (!AccessControl.canAccessModule("PRODUCTS") && !AccessControl.canAccessModule("POS")) {
            throw new SecurityException("Access denied to PRODUCTS module.");
        }
        return productDAO.findById(id);
    }

    public int createProduct(Product product) throws SQLException {
        AccessControl.requireWrite("PRODUCTS");
        return productDAO.create(product);
    }

    public boolean updateProduct(Product product) throws SQLException {
        AccessControl.requireWrite("PRODUCTS");
        return productDAO.update(product);
    }

    public boolean deleteProduct(int id) throws SQLException {
        AccessControl.requireWrite("PRODUCTS");
        return productDAO.delete(id);
    }
}
