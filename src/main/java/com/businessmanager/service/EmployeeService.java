package com.businessmanager.service;

import com.businessmanager.dao.EmployeeDAO;
import com.businessmanager.model.Employee;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class EmployeeService {

    private final EmployeeDAO employeeDAO;

    public EmployeeService() {
        this.employeeDAO = new EmployeeDAO();
    }

    public EmployeeService(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    public List<Employee> getEmployeesForBusiness(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("EMPLOYEES")) {
            throw new SecurityException("Access denied to EMPLOYEES module.");
        }
        return employeeDAO.findByBusiness(businessId);
    }

    public Employee getEmployeeById(int id) throws SQLException {
        if (!AccessControl.canAccessModule("EMPLOYEES")) {
            throw new SecurityException("Access denied to EMPLOYEES module.");
        }
        return employeeDAO.findById(id);
    }

    public int createEmployee(Employee employee) throws SQLException {
        AccessControl.requireWrite("EMPLOYEES");
        return employeeDAO.create(employee);
    }

    public boolean updateEmployee(Employee employee) throws SQLException {
        AccessControl.requireWrite("EMPLOYEES");
        return employeeDAO.update(employee);
    }

    public boolean deleteEmployee(int id) throws SQLException {
        AccessControl.requireWrite("EMPLOYEES");
        return employeeDAO.delete(id);
    }
}
