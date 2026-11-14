package com.businessmanager.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class RecentSaleDTO {
    private int saleId;
    private String invoiceNumber;
    private Timestamp saleDate;
    private String customerName;
    private int itemCount;
    private BigDecimal totalAmount;
    private String paymentStatus;

    public RecentSaleDTO() {}

    public RecentSaleDTO(int saleId, String invoiceNumber, Timestamp saleDate, String customerName, int itemCount, BigDecimal totalAmount, String paymentStatus) {
        this.saleId = saleId;
        this.invoiceNumber = invoiceNumber;
        this.saleDate = saleDate;
        this.customerName = customerName;
        this.itemCount = itemCount;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Timestamp getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(Timestamp saleDate) {
        this.saleDate = saleDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
