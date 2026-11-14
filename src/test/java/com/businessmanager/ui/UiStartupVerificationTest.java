package com.businessmanager.ui;

import com.businessmanager.enums.BusinessType;
import com.businessmanager.enums.UserRole;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.AppTheme;
import com.businessmanager.ui.analytics.AnalyticsPanel;
import com.businessmanager.ui.auth.BusinessInfoPanel;
import com.businessmanager.ui.auth.BusinessTypePanel;
import com.businessmanager.ui.auth.CreateAccountPanel;
import com.businessmanager.ui.auth.LoginPanel;
import com.businessmanager.ui.branches.BranchManagementPanel;
import com.businessmanager.ui.categories.CategoryManagementPanel;
import com.businessmanager.ui.customers.CustomerManagementPanel;
import com.businessmanager.ui.dashboard.LargeBusinessDashboardPanel;
import com.businessmanager.ui.dashboard.SmallBusinessDashboardPanel;
import com.businessmanager.ui.employees.EmployeeManagementPanel;
import com.businessmanager.ui.expenses.ExpenseManagementPanel;
import com.businessmanager.ui.inventory.InventoryManagementPanel;
import com.businessmanager.ui.payments.PaymentManagementPanel;
import com.businessmanager.ui.pos.POSBillingPanel;
import com.businessmanager.ui.products.ProductManagementPanel;
import com.businessmanager.ui.purchases.PurchaseManagementPanel;
import com.businessmanager.ui.reports.ReportsPanel;
import com.businessmanager.ui.suppliers.SupplierManagementPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

@Tag("ui")
class UiStartupVerificationTest {

    @BeforeAll
    static void setupTheme() {
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                AppTheme.setup();
            } catch (Exception ignored) {}
        }
    }

    @AfterEach
    void clear() {
        UserSession.clearSession();
    }

    @Test
    void instantiatesAllUiPanelsWithoutErrors() {
        User owner = new User();
        owner.setId(1);
        owner.setFullName("Test Owner");
        owner.setRole(UserRole.OWNER);
        Business business = new Business(1, "Test Enterprise", BusinessType.LARGE, "test@enterprise.com");
        UserSession.startSession(owner, business);

        assertDoesNotThrow(() -> new WelcomePanel(null));
        assertDoesNotThrow(() -> new LoginPanel(null));
        assertDoesNotThrow(() -> new BusinessTypePanel(null));
        assertDoesNotThrow(() -> new CreateAccountPanel(null));
        assertDoesNotThrow(() -> new BusinessInfoPanel(null));
        assertDoesNotThrow(SmallBusinessDashboardPanel::new);
        assertDoesNotThrow(LargeBusinessDashboardPanel::new);
        assertDoesNotThrow(ProductManagementPanel::new);
        assertDoesNotThrow(CategoryManagementPanel::new);
        assertDoesNotThrow(InventoryManagementPanel::new);
        assertDoesNotThrow(POSBillingPanel::new);
        assertDoesNotThrow(CustomerManagementPanel::new);
        assertDoesNotThrow(SupplierManagementPanel::new);
        assertDoesNotThrow(PurchaseManagementPanel::new);
        assertDoesNotThrow(PaymentManagementPanel::new);
        assertDoesNotThrow(ExpenseManagementPanel::new);
        assertDoesNotThrow(EmployeeManagementPanel::new);
        assertDoesNotThrow(BranchManagementPanel::new);
        assertDoesNotThrow(ReportsPanel::new);
        assertDoesNotThrow(AnalyticsPanel::new);
    }

    @Test
    void instantiatesMainFrameWithoutErrors() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping Swing JFrame test in headless environment");

        User owner = new User();
        owner.setId(1);
        owner.setFullName("Test Owner");
        owner.setRole(UserRole.OWNER);
        Business business = new Business(1, "Test Enterprise", BusinessType.LARGE, "test@enterprise.com");
        UserSession.startSession(owner, business);

        assertDoesNotThrow(() -> {
            MainFrame frame = new MainFrame();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            assertNotNull(frame.getTitle());
            frame.dispose();
        });
    }
}
