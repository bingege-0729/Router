package com.javaee.backend.entity.geo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Bounds {
    private Location southwest;  // 西南角
    private Location northeast;  // 东北角

    // 是否包含某点
    public boolean contains(Location loc) {
        return loc.getLat() >= southwest.getLat() &&
                loc.getLat() <= northeast.getLat() &&
                loc.getLng() >= southwest.getLng() &&
                loc.getLng() <= northeast.getLng();
    }

    // 扩展边界
    public void expand(double meters) {
        // 简单实现：经纬度各扩展约0.01度 ≈ 1km
        double delta = meters / 111000.0;
        southwest = new Location(
                southwest.getLat() - delta,
                southwest.getLng() - delta
        );
        northeast = new Location(
                northeast.getLat() + delta,
                northeast.getLng() + delta
        );
    }
}