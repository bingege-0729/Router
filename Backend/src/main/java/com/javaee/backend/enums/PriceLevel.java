package com.javaee.backend.enums;



public enum PriceLevel {
    CHEAP("💰", 0, 50, "便宜"),
    MEDIUM("💰💰", 51, 150, "中等"),
    EXPENSIVE("💰💰💰", 151, 300, "较贵"),
    LUXURY("💰💰💰💰", 301, Integer.MAX_VALUE, "奢华");

    private final String icon;
    private final int min;
    private final int max;
    private final String desc;

    PriceLevel(String icon, int min, int max, String desc) {
        this.icon = icon;
        this.min = min;
        this.max = max;
        this.desc = desc;
    }

    public static PriceLevel fromPrice(double price) {
        if (price <= CHEAP.max) return CHEAP;
        if (price <= MEDIUM.max) return MEDIUM;
        if (price <= EXPENSIVE.max) return EXPENSIVE;
        return LUXURY;
    }
}
