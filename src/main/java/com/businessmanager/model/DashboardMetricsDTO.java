package com.businessmanager.model;

import java.math.BigDecimal;

public class DashboardMetricsDTO {
    private BigDecimal totalSales = BigDecimal.ZERO;
    private double salesChangePercent = 0.0;
    private BigDecimal grossProfit = BigDecimal.ZERO;
    private double profitChangePercent = 0.0;
    private BigDecimal totalPurchases = BigDecimal.ZERO;
    private double purchasesChangePercent = 0.0;
    private BigDecimal inventoryValue = BigDecimal.ZERO;
    private BigDecimal outstandingPayments = BigDecimal.ZERO;
    private int totalCustomers = 0;

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales != null ? totalSales : BigDecimal.ZERO;
    }

    public double getSalesChangePercent() {
        return salesChangePercent;
    }

    public void setSalesChangePercent(double salesChangePercent) {
        this.salesChangePercent = salesChangePercent;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit != null ? grossProfit : BigDecimal.ZERO;
    }

    public double getProfitChangePercent() {
        return profitChangePercent;
    }

    public void setProfitChangePercent(double profitChangePercent) {
        this.profitChangePercent = profitChangePercent;
    }

    public BigDecimal getTotalPurchases() {
        return totalPurchases;
    }

    public void setTotalPurchases(BigDecimal totalPurchases) {
        this.totalPurchases = totalPurchases != null ? totalPurchases : BigDecimal.ZERO;
    }

    public double getPurchasesChangePercent() {
        return purchasesChangePercent;
    }

    public void setPurchasesChangePercent(double purchasesChangePercent) {
        this.purchasesChangePercent = purchasesChangePercent;
    }

    public BigDecimal getInventoryValue() {
        return inventoryValue;
    }

    public void setInventoryValue(BigDecimal inventoryValue) {
        this.inventoryValue = inventoryValue != null ? inventoryValue : BigDecimal.ZERO;
    }

    public BigDecimal getOutstandingPayments() {
        return outstandingPayments;
    }

    public void setOutstandingPayments(BigDecimal outstandingPayments) {
        this.outstandingPayments = outstandingPayments != null ? outstandingPayments : BigDecimal.ZERO;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(int totalCustomers) {
        this.totalCustomers = totalCustomers;
    }
}
