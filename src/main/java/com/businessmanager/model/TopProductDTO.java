package com.businessmanager.model;

import java.math.BigDecimal;

public class TopProductDTO {
    private int rank;
    private int productId;
    private String productName;
    private String categoryName;
    private int quantitySold;
    private BigDecimal revenue;

    public TopProductDTO() {}

    public TopProductDTO(int rank, int productId, String productName, String categoryName, int quantitySold, BigDecimal revenue) {
        this.rank = rank;
        this.productId = productId;
        this.productName = productName;
        this.categoryName = categoryName;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
