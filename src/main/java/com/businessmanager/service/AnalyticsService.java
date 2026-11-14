package com.businessmanager.service;

import com.businessmanager.dao.AnalyticsDAO;
import com.businessmanager.security.AccessControl;
import org.jfree.data.category.DefaultCategoryDataset;

import java.sql.SQLException;

public class AnalyticsService {

    private final AnalyticsDAO analyticsDAO;

    public AnalyticsService() {
        this.analyticsDAO = new AnalyticsDAO();
    }

    public AnalyticsService(AnalyticsDAO analyticsDAO) {
        this.analyticsDAO = analyticsDAO;
    }

    public DefaultCategoryDataset getTopProductsDataset(int businessId, Integer branchId) throws SQLException {
        if (!AccessControl.canAccessModule("DASHBOARD")) {
            throw new SecurityException("Access denied to Analytics.");
        }
        return analyticsDAO.getTopProductsDataset(businessId, branchId);
    }

    public DefaultCategoryDataset getCategoryRevenueDataset(int businessId, Integer branchId) throws SQLException {
        if (!AccessControl.canAccessModule("DASHBOARD")) {
            throw new SecurityException("Access denied to Analytics.");
        }
        return analyticsDAO.getCategoryRevenueDataset(businessId, branchId);
    }

    public DefaultCategoryDataset getBranchComparisonDataset(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("DASHBOARD")) {
            throw new SecurityException("Access denied to Analytics.");
        }
        return analyticsDAO.getBranchComparisonDataset(businessId);
    }

    public DefaultCategoryDataset getMonthlyRevenueTrendDataset(int businessId, Integer branchId) throws SQLException {
        if (!AccessControl.canAccessModule("DASHBOARD")) {
            throw new SecurityException("Access denied to Analytics.");
        }
        return analyticsDAO.getMonthlyRevenueTrendDataset(businessId, branchId);
    }

    public DefaultCategoryDataset getExpenseCategoryBreakdownDataset(int businessId, Integer branchId) throws SQLException {
        if (!AccessControl.canAccessModule("DASHBOARD")) {
            throw new SecurityException("Access denied to Analytics.");
        }
        return analyticsDAO.getExpenseCategoryBreakdownDataset(businessId, branchId);
    }

    public DefaultCategoryDataset getTopCustomersDataset(int businessId, Integer branchId) throws SQLException {
        if (!AccessControl.canAccessModule("DASHBOARD")) {
            throw new SecurityException("Access denied to Analytics.");
        }
        return analyticsDAO.getTopCustomersDataset(businessId, branchId);
    }
}
