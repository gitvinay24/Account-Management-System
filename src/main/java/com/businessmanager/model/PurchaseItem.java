package com.businessmanager.model;

import java.math.BigDecimal;

public class PurchaseItem {
    private int id;
    private int purchaseId;
    private int productId;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private int quantity;
    private BigDecimal lineTotal = BigDecimal.ZERO;

    // Display fields
    private String productName;
    private String productSku;

    public PurchaseItem() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
}
