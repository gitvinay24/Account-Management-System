package com.businessmanager.service;

import com.businessmanager.model.Coupon;
import com.businessmanager.model.PosCalculationResult;
import com.businessmanager.model.SaleItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Single authoritative calculation engine for POS billing totals, coupons, GST, and payment change.
 */
public class PosCalculationEngine {

    private final CouponService couponService = new CouponService();

    public PosCalculationResult calculate(List<SaleItem> items, String couponCode, boolean gstEnabled, BigDecimal gstRate, BigDecimal paymentReceived) {
        PosCalculationResult result = new PosCalculationResult();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal itemDiscounts = BigDecimal.ZERO;

        if (items != null) {
            for (SaleItem item : items) {
                if (item == null) continue;
                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                int qty = Math.max(0, item.getQuantity());
                BigDecimal lineBase = price.multiply(BigDecimal.valueOf(qty));
                BigDecimal itemDisc = item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO;

                subtotal = subtotal.add(lineBase);
                itemDiscounts = itemDiscounts.add(itemDisc);
            }
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        itemDiscounts = itemDiscounts.setScale(2, RoundingMode.HALF_UP);

        result.setSubtotal(subtotal);
        result.setItemDiscount(itemDiscounts);

        // Coupon calculation
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Coupon coupon = couponService.findCoupon(couponCode);
            if (coupon != null && coupon.isActive()) {
                if (subtotal.compareTo(coupon.getMinPurchase()) >= 0) {
                    result.setCouponCode(coupon.getCode());
                    if (coupon.getType() == Coupon.DiscountType.PERCENTAGE) {
                        couponDiscount = subtotal.multiply(coupon.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    } else if (coupon.getType() == Coupon.DiscountType.FLAT) {
                        couponDiscount = coupon.getValue().min(subtotal);
                    }
                }
            }
        }
        couponDiscount = couponDiscount.setScale(2, RoundingMode.HALF_UP);
        result.setCouponDiscount(couponDiscount);

        BigDecimal totalDiscount = itemDiscounts.add(couponDiscount).setScale(2, RoundingMode.HALF_UP);
        result.setTotalDiscount(totalDiscount);

        BigDecimal taxableAmount = subtotal.subtract(couponDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        result.setTaxableAmount(taxableAmount);

        // GST calculation
        BigDecimal rate = (gstRate != null && gstRate.signum() >= 0) ? gstRate : new BigDecimal("18.00");
        result.setGstEnabled(gstEnabled);
        result.setGstRate(rate);

        BigDecimal gstAmount = new BigDecimal("0.00");
        if (gstEnabled && rate.signum() > 0) {
            gstAmount = taxableAmount.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        result.setGstAmount(gstAmount);

        BigDecimal grandTotal = taxableAmount.add(gstAmount).setScale(2, RoundingMode.HALF_UP);
        result.setGrandTotal(grandTotal);

        BigDecimal tender = paymentReceived != null ? paymentReceived.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        result.setPaymentReceived(tender);

        BigDecimal change = tender.subtract(grandTotal).max(new BigDecimal("0.00")).setScale(2, RoundingMode.HALF_UP);
        result.setChangeAmount(change);

        result.setPaymentSufficient(tender.compareTo(grandTotal) >= 0 && grandTotal.signum() > 0);

        return result;
    }
}
