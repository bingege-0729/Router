package com.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;


@Slf4j
@Component
public class DistanceUtil {
    
    private static final double EARTH_RADIUS = 6371000; // 地球半径（米）
    
    /**
     * 计算两点之间的球面距离（哈弗辛公式）
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
    
    /**
     * 根据距离估算交通时间（分钟）
     */
    public static int estimateTravelTime(double distanceMeters, String transportMode) {
        double speed; // 米/分钟
        switch (transportMode) {
            case "WALK":
                speed = 80;   // 4.8 km/h
                break;
            case "BIKE":
                speed = 200;  // 12 km/h
                break;
            case "DRIVE":
                speed = 400;  // 24 km/h（考虑红绿灯）
                break;
            default:
                speed = 80;
        }
        return (int) Math.ceil(distanceMeters / speed);
    }
    
    /**
     * 解析BigDecimal经纬度
     */
    public static double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0;
    }
}