package com.businessmanager.ui;

import com.businessmanager.components.SidebarNav;
import com.businessmanager.database.DatabaseInitializer;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import com.businessmanager.security.UserSession;
import com.businessmanager.security.AccessControl;
import com.businessmanager.theme.AppTheme;
import com.businessmanager.theme.UiStyles;
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
import com.businessmanager.ui.sales.SalesHistoryPanel;
import com.businessmanager.ui.settings.SettingsPanel;
import com.businessmanager.ui.suppliers.SupplierManagementPanel;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public static final String WELCOME = "WELCOME";
    public static final String LOGIN = "LOGIN";
    public static final String CREATE_ACCOUNT = "CREATE_ACCOUNT";
    public static final String BUSINESS_INFO = "BUSINESS_INFO";
    public static final String BUSINESS_TYPE = "BUSINESS_TYPE";
    public static final String MAIN_APP = "MAIN_APP";

    private final CardLayout cardLayout;
    private final JPanel screenContainer;

    private BusinessInfoPanel businessInfoPanel;
    private BusinessTypePanel businessTypePanel;

    private JPanel mainAppPanel;
    private CardLayout mainAppCardLayout;
    private JPanel mainContentPanel;
    private SidebarNav sidebarNav;

    public MainFrame() {
        AppTheme.setup();

        setTitle("Account Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 920);
        // The auth shell keeps its centered form usable at laptop-sized windows.
        setMinimumSize(new Dimension(900, 680));
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        screenContainer = new JPanel(cardLayout);

        buildAuthScreens();
        UiStyles.apply(screenContainer);
        setContentPane(screenContainer);

        showScreen(WELCOME);
    }

    /** Verifies persistence only when a registered user needs it; guest mode is fully local. */
    public boolean ensureDatabaseReadyForRegisteredAccess() {
        if (DatabaseInitializer.initializeDatabase()) return true;
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this,
                    "Account Management could not reach its MySQL database.\n\n"
                            + "Start MySQL and verify the credentials in db.properties.",
                    "Database connection required", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private void buildAuthScreens() {
        screenContainer.add(new WelcomePanel(this), WELCOME);
        screenContainer.add(new LoginPanel(this), LOGIN);
        screenContainer.add(new CreateAccountPanel(this), CREATE_ACCOUNT);

        businessInfoPanel = new BusinessInfoPanel(this);
        screenContainer.add(businessInfoPanel, BUSINESS_INFO);

        businessTypePanel = new BusinessTypePanel(this);
        screenContainer.add(businessTypePanel, BUSINESS_TYPE);
    }

    public void showScreen(String screen) {
        cardLayout.show(screenContainer, screen);
    }

    public void passUserToBusinessInfo(User user) {
        if (businessInfoPanel != null) {
            businessInfoPanel.setDraftUser(user);
        }
        showScreen(BUSINESS_INFO);
    }

    public void passRegistrationToBusinessType(User user, Business business) {
        if (businessTypePanel != null) {
            businessTypePanel.setRegistrationData(user, business);
        }
        showScreen(BUSINESS_TYPE);
    }

    public void onLoginSuccess() {
        if (mainAppPanel != null) {
            screenContainer.remove(mainAppPanel);
        }

        mainAppPanel = new JPanel(new BorderLayout());
        mainAppCardLayout = new CardLayout();
        mainContentPanel = new JPanel(mainAppCardLayout);

        sidebarNav = new SidebarNav(this, this::navigateToModule);

        // Load modules
        if (UserSession.isLargeBusiness()) {
            mainContentPanel.add(new LargeBusinessDashboardPanel(), "DASHBOARD");
        } else {
            mainContentPanel.add(new SmallBusinessDashboardPanel(), "DASHBOARD");
        }

        mainContentPanel.add(new POSBillingPanel(), "POS");
        mainContentPanel.add(new ProductManagementPanel(), "PRODUCTS");
        mainContentPanel.add(new CategoryManagementPanel(), "CATEGORIES");
        mainContentPanel.add(new InventoryManagementPanel(), "INVENTORY");
        mainContentPanel.add(new SalesHistoryPanel(), "SALES");
        mainContentPanel.add(new PurchaseManagementPanel(), "PURCHASES");
        mainContentPanel.add(new CustomerManagementPanel(), "CUSTOMERS");
        mainContentPanel.add(new SupplierManagementPanel(), "SUPPLIERS");
        mainContentPanel.add(new PaymentManagementPanel(), "PAYMENTS");
        mainContentPanel.add(new ExpenseManagementPanel(), "EXPENSES");

        if (UserSession.isLargeBusiness()) {
            mainContentPanel.add(new EmployeeManagementPanel(), "EMPLOYEES");
            mainContentPanel.add(new BranchManagementPanel(), "BRANCHES");
        }

        mainContentPanel.add(new ReportsPanel(), "REPORTS");
        mainContentPanel.add(new AnalyticsPanel(), "ANALYTICS");
        mainContentPanel.add(new SettingsPanel(), "SETTINGS");

        mainAppPanel.add(sidebarNav, BorderLayout.WEST);
        mainAppPanel.add(mainContentPanel, BorderLayout.CENTER);
        if (UserSession.isGuestSession()) {
            JLabel guestBanner = new JLabel("Guest sample access — sample data only; changes are disabled.", SwingConstants.CENTER);
            guestBanner.setOpaque(true);
            guestBanner.setBackground(com.businessmanager.theme.Colors.PRIMARY_CONTAINER);
            guestBanner.setForeground(com.businessmanager.theme.Colors.ON_PRIMARY_CONTAINER);
            guestBanner.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            mainAppPanel.add(guestBanner, BorderLayout.NORTH);
        }

        screenContainer.add(mainAppPanel, MAIN_APP);
        UiStyles.apply(mainAppPanel);
        cardLayout.show(screenContainer, MAIN_APP);
    }

    private void navigateToModule(String module) {
        if (!AccessControl.canAccessModule(module)) {
            JOptionPane.showMessageDialog(this, "Your role does not have access to this module.", "Access denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        mainAppCardLayout.show(mainContentPanel, module);
    }

    public void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of your session?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            UserSession.clearSession();

            if (mainAppPanel != null) {
                screenContainer.remove(mainAppPanel);
                mainAppPanel = null;
            }

            for (Component comp : screenContainer.getComponents()) {
                if (comp instanceof LoginPanel loginPanel) {
                    loginPanel.resetForm();
                }
            }

            showScreen(LOGIN);
        }
    }
}
