package com.javaee.backend.enums;



public enum TransportMode {
    WALK("步行", 5.0, "适合1km以内"),
    BIKE("骑行", 3.0, "适合1-3km"),
    TAXI("打车", 2.0, "速度快但贵"),
    BUS("公交", 1.5, "便宜但慢"),
    SUBWAY("地铁", 2.5, "快且便宜"),
    DRIVE("自驾", 0, "需考虑停车");

    private final String name;
    private final double speedKmPerMin;  // 速度(公里/分钟)
    private final String tip;

    TransportMode(String name, double speedKmPerMin, String tip) {
        this.name = name;
        this.speedKmPerMin = speedKmPerMin;
        this.tip = tip;
    }

    public int estimateTime(int distanceMeters) {
        return (int) (distanceMeters / 1000.0 / speedKmPerMin);
    }
}
