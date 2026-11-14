package com.businessmanager.service;

import com.businessmanager.dao.CustomerDAO;
import com.businessmanager.model.Customer;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class CustomerService {

    private final CustomerDAO customerDAO;

    public CustomerService() {
        this.customerDAO = new CustomerDAO();
    }

    public CustomerService(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    public List<Customer> getCustomersForBusiness(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("CUSTOMERS")) {
            throw new SecurityException("Access denied to CUSTOMERS module.");
        }
        return customerDAO.findByBusiness(businessId);
    }

    public Customer getCustomerById(int id) throws SQLException {
        if (!AccessControl.canAccessModule("CUSTOMERS")) {
            throw new SecurityException("Access denied to CUSTOMERS module.");
        }
        return customerDAO.findById(id);
    }

    public int createCustomer(Customer customer) throws SQLException {
        AccessControl.requireWrite("CUSTOMERS");
        return customerDAO.create(customer);
    }

    public boolean updateCustomer(Customer customer) throws SQLException {
        AccessControl.requireWrite("CUSTOMERS");
        return customerDAO.update(customer);
    }

    public boolean deleteCustomer(int id) throws SQLException {
        AccessControl.requireWrite("CUSTOMERS");
        return customerDAO.delete(id);
    }
}
