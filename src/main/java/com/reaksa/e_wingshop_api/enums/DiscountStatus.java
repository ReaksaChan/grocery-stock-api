package com.reaksa.e_wingshop_api.enums;

public enum DiscountStatus {
    ACTIVE,     // currently applied to inventory
    EXPIRED,    // the item has now passed its expiry date
    REVOKED,    // manually removed by admin
    SOLD_OUT    // stock reached zero while discount was active
}
