package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.enums.PaymentStatus;
import com.businessmanager.model.Purchase;
import com.businessmanager.model.Sale;
import com.businessmanager.security.AccessControl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Persists payment-ledger records and settles outstanding sales and purchases atomically. */
public class PaymentDAO {

    public List<Sale> findOutstandingSales(int businessId) throws SQLException {
        List<Sale> sales = new SaleDAO().findByBusiness(businessId);
        List<Sale> outstanding = new ArrayList<>();
        for (Sale sale : sales) {
            if (sale.getCustomerId() != null && sale.getTotal().compareTo(sale.getPaidAmount()) > 0) outstanding.add(sale);
        }
        return outstanding;
    }

    public List<Purchase> findOutstandingPurchases(int businessId) throws SQLException {
        List<Purchase> purchases = new PurchaseDAO().findByBusiness(businessId);
        List<Purchase> outstanding = new ArrayList<>();
        for (Purchase purchase : purchases) {
            if (purchase.getTotal().compareTo(purchase.getPaidAmount()) > 0) outstanding.add(purchase);
        }
        return outstanding;
    }

    public void settleSale(int businessId, int saleId, BigDecimal amount, PaymentMethod method, String notes) throws SQLException {
        AccessControl.requireWrite("PAYMENTS");
        if (DemoModeConfig.isEnabled()) { DemoDataStore.settleSale(saleId, amount, method, notes); return; }
        settle(businessId, saleId, amount, method, notes, true);
    }

    public void settlePurchase(int businessId, int purchaseId, BigDecimal amount, PaymentMethod method, String notes) throws SQLException {
        AccessControl.requireWrite("PAYMENTS");
        if (DemoModeConfig.isEnabled()) { DemoDataStore.settlePurchase(purchaseId, amount, method, notes); return; }
        settle(businessId, purchaseId, amount, method, notes, false);
    }

    private void settle(int businessId, int documentId, BigDecimal amount, PaymentMethod method, String notes, boolean sale) throws SQLException {
        if (amount == null || amount.signum() <= 0) throw new SQLException("Payment amount must be positive.");
        if (method == null) throw new SQLException("Payment method is required.");

        String table = sale ? "sales" : "purchases";
        String partyColumn = sale ? "customer_id" : "supplier_id";
        String partyType = sale ? "CUSTOMER" : "SUPPLIER";
        String referenceType = sale ? "SALE" : "PURCHASE";
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int partyId;
                BigDecimal total;
                BigDecimal paid;
                String lockSql = "SELECT " + partyColumn + ", total, paid_amount FROM " + table + " WHERE id = ? AND business_id = ? FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(lockSql)) {
                    statement.setInt(1, documentId);
                    statement.setInt(2, businessId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("The selected document was not found.");
                        partyId = result.getInt(1);
                        if (result.wasNull()) throw new SQLException("Walk-in sales cannot receive a later account payment.");
                        total = result.getBigDecimal(2);
                        paid = result.getBigDecimal(3);
                    }
                }
                BigDecimal outstanding = total.subtract(paid);
                if (amount.compareTo(outstanding) > 0) throw new SQLException("Payment exceeds the outstanding balance of " + outstanding + ".");
                BigDecimal newPaid = paid.add(amount);
                PaymentStatus status = newPaid.compareTo(total) == 0 ? PaymentStatus.PAID : PaymentStatus.PARTIAL;

                try (PreparedStatement statement = connection.prepareStatement("UPDATE " + table + " SET paid_amount = ?, payment_status = ? WHERE id = ?")) {
                    statement.setBigDecimal(1, newPaid);
                    statement.setString(2, status.name());
                    statement.setInt(3, documentId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO payments (business_id, party_type, party_id, reference_type, reference_id, amount, payment_method, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    statement.setInt(1, businessId);
                    statement.setString(2, partyType);
                    statement.setInt(3, partyId);
                    statement.setString(4, referenceType);
                    statement.setInt(5, documentId);
                    statement.setBigDecimal(6, amount);
                    statement.setString(7, method.name());
                    statement.setString(8, notes == null || notes.isBlank() ? "Direct settlement" : notes.trim());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
