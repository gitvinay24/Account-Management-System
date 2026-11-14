package com.businessmanager.model;

import java.math.BigDecimal;

public class PosCalculationResult {

    private BigDecimal subtotal = new BigDecimal("0.00");
    private BigDecimal itemDiscount = new BigDecimal("0.00");
    private String couponCode;
    private BigDecimal couponDiscount = new BigDecimal("0.00");
    private BigDecimal totalDiscount = new BigDecimal("0.00");
    private BigDecimal taxableAmount = new BigDecimal("0.00");
    private boolean gstEnabled = false;
    private BigDecimal gstRate = new BigDecimal("0.00");
    private BigDecimal gstAmount = new BigDecimal("0.00");
    private BigDecimal grandTotal = new BigDecimal("0.00");
    private BigDecimal paymentReceived = new BigDecimal("0.00");
    private BigDecimal changeAmount = new BigDecimal("0.00");
    private boolean paymentSufficient = false;

    public PosCalculationResult() {
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getItemDiscount() { return itemDiscount; }
    public void setItemDiscount(BigDecimal itemDiscount) { this.itemDiscount = itemDiscount; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }

    public BigDecimal getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; }

    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; }

    public boolean isGstEnabled() { return gstEnabled; }
    public void setGstEnabled(boolean gstEnabled) { this.gstEnabled = gstEnabled; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public BigDecimal getPaymentReceived() { return paymentReceived; }
    public void setPaymentReceived(BigDecimal paymentReceived) { this.paymentReceived = paymentReceived; }

    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; }

    public boolean isPaymentSufficient() { return paymentSufficient; }
    public void setPaymentSufficient(boolean paymentSufficient) { this.paymentSufficient = paymentSufficient; }
}
