package com.businessmanager.security;

import com.businessmanager.enums.BusinessType;
import com.businessmanager.enums.UserRole;
import com.businessmanager.model.Business;
import com.businessmanager.model.Employee;
import com.businessmanager.model.User;
import com.businessmanager.service.EmployeeService;
import com.businessmanager.service.ProductService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServicePermissionTest {

    @AfterEach
    void clear() {
        UserSession.clearSession();
    }

    @Test
    void cashierCannotAccessOrModifyEmployeeService() {
        User cashier = new User();
        cashier.setRole(UserRole.CASHIER);
        Business business = new Business(1, "Test Business", BusinessType.SMALL, "test@example.com");
        UserSession.startSession(cashier, business);

        EmployeeService employeeService = new EmployeeService();
        assertThrows(SecurityException.class, () -> employeeService.getEmployeesForBusiness(1));
        assertThrows(SecurityException.class, () -> employeeService.createEmployee(new Employee()));
    }

    @Test
    void cashierCanAccessProductReadMethodsButCannotModifyProductService() {
        User cashier = new User();
        cashier.setRole(UserRole.CASHIER);
        Business business = new Business(1, "Test Business", BusinessType.SMALL, "test@example.com");
        UserSession.startSession(cashier, business);

        ProductService productService = new ProductService();
        assertThrows(SecurityException.class, () -> productService.getProductsForBusiness(1));
        assertThrows(SecurityException.class, () -> productService.createProduct(null));
    }
}
