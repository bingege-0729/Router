package com.javaee.backend.entity.geo;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Location {
    private Double lat;      // 纬度
    private Double lng;      // 经度

    // 计算两点距离(米) - 哈弗辛公式
    public double distanceTo(Location other) {
        double R = 6371000; // 地球半径(米)
        double dLat = Math.toRadians(other.lat - this.lat);
        double dLng = Math.toRadians(other.lng - this.lng);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(other.lat)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    public static Location of(double lat, double lng) {
        return new Location(lat, lng);
    }
}