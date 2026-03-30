package com.reaksa.e_wingshop_api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Expiry-based discount tiers.
 * Each tier defines the expiry window (days) and the default discount percentage.
 */
@Getter
@RequiredArgsConstructor
public enum DiscountTier {

    ONE_MONTH  ("Expiring in 1 month",  30, 10),  // 10% off
    TWO_WEEKS  ("Expiring in 2 weeks",  14, 20),  // 20% off
    ONE_WEEK   ("Expiring in 1 week",    7, 35),  // 35% off
    THREE_DAYS ("Expiring in 3 days",    3, 50),  // 50% off
    CUSTOM     ("Custom window",         0,  0);  // admin sets both fields

    private final String label;
    private final int    daysWindow;       // items expiring within this many days
    private final int    defaultRatePct;   // suggested discount %
}
