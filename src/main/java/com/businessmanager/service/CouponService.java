package com.businessmanager.service;

import com.businessmanager.model.Coupon;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Centralized service for validating and looking up promotional coupons and offers. */
public class CouponService {

    private final Map<String, Coupon> couponRegistry = Collections.synchronizedMap(new LinkedHashMap<>());

    public CouponService() {
        // Default production offers
        registerCoupon(new Coupon("WELCOME10", "10% Welcome Discount", Coupon.DiscountType.PERCENTAGE, new BigDecimal("10.00"), BigDecimal.ZERO));
        registerCoupon(new Coupon("FLAT500", "₹500 Flat Savings", Coupon.DiscountType.FLAT, new BigDecimal("500.00"), new BigDecimal("2000.00")));
        registerCoupon(new Coupon("FESTIVE15", "15% Festive Offer", Coupon.DiscountType.PERCENTAGE, new BigDecimal("15.00"), new BigDecimal("1000.00")));
        registerCoupon(new Coupon("SUPER20", "20% Super Saver", Coupon.DiscountType.PERCENTAGE, new BigDecimal("20.00"), new BigDecimal("5000.00")));
    }

    public void registerCoupon(Coupon coupon) {
        if (coupon != null && coupon.getCode() != null) {
            couponRegistry.put(coupon.getCode().trim().toUpperCase(), coupon);
        }
    }

    public Coupon findCoupon(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        return couponRegistry.get(code.trim().toUpperCase());
    }

    public Map<String, Coupon> getAllCoupons() {
        return Collections.unmodifiableMap(couponRegistry);
    }
}
