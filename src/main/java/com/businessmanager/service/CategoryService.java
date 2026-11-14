package com.businessmanager.service;

import com.businessmanager.dao.CategoryDAO;
import com.businessmanager.model.Category;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }

    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    public List<Category> getCategoriesForBusiness(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("CATEGORIES")) {
            throw new SecurityException("Access denied to CATEGORIES module.");
        }
        return categoryDAO.findByBusiness(businessId);
    }

    public Category getCategoryById(int id) throws SQLException {
        if (!AccessControl.canAccessModule("CATEGORIES")) {
            throw new SecurityException("Access denied to CATEGORIES module.");
        }
        return categoryDAO.findById(id);
    }

    public int createCategory(Category category) throws SQLException {
        AccessControl.requireWrite("CATEGORIES");
        return categoryDAO.create(category);
    }

    public boolean updateCategory(Category category) throws SQLException {
        AccessControl.requireWrite("CATEGORIES");
        return categoryDAO.update(category);
    }

    public boolean deleteCategory(int id) throws SQLException {
        AccessControl.requireWrite("CATEGORIES");
        return categoryDAO.delete(id);
    }
}
