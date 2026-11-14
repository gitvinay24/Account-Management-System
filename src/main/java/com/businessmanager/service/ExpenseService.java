package com.businessmanager.service;

import com.businessmanager.dao.ExpenseDAO;
import com.businessmanager.model.Expense;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class ExpenseService {

    private final ExpenseDAO expenseDAO;

    public ExpenseService() {
        this.expenseDAO = new ExpenseDAO();
    }

    public ExpenseService(ExpenseDAO expenseDAO) {
        this.expenseDAO = expenseDAO;
    }

    public List<Expense> getExpensesForBusiness(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("EXPENSES")) {
            throw new SecurityException("Access denied to EXPENSES module.");
        }
        return expenseDAO.findByBusiness(businessId);
    }

    public int createExpense(Expense expense) throws SQLException {
        AccessControl.requireWrite("EXPENSES");
        return expenseDAO.create(expense);
    }

    public boolean updateExpense(Expense expense) throws SQLException {
        AccessControl.requireWrite("EXPENSES");
        return expenseDAO.update(expense);
    }

    public boolean deleteExpense(int id) throws SQLException {
        AccessControl.requireWrite("EXPENSES");
        return expenseDAO.delete(id);
    }
}
