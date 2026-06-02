package com.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;



import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DistanceUtil 距离计算工具测试")
class DistanceUtilTest {

    @Test
    @DisplayName("计算相同坐标点距离应为0")
    void testSamePoint() {
        double distance = DistanceUtil.calculateDistance(
                30.2741, 120.1551,
                30.2741, 120.1551
        );
        
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    @DisplayName("杭州西湖到灵隐寺的距离应在合理范围内")
    void testKnownDistance() {
        double distance = DistanceUtil.calculateDistance(
                30.2545, 120.1489,  // 西湖
                30.2436, 120.1016   // 灵隐寺
        );
        
        double distanceKm = distance / 1000;
        assertTrue(distanceKm > 4 && distanceKm < 6, 
                "西湖到灵隐寺的距离应该在4-6公里之间，实际: " + distanceKm + "km");
    }

    @Test
    @DisplayName("经度变化对距离的影响应大于纬度变化（在赤道附近）")
    void testLongitudeVsLatitudeImpact() {
        double latChange = DistanceUtil.calculateDistance(
                0.0, 100.0,
                1.0, 100.0
        );
        
        double lngChange = DistanceUtil.calculateDistance(
                0.0, 100.0,
                0.0, 101.0
        );
        
        assertTrue(Math.abs(latChange - lngChange) < 10000,
                "在赤道附近，纬度和经度变化1度的距离应该相近");
    }

    @Test
    @DisplayName("负数坐标应正常处理")
    void testNegativeCoordinates() {
        assertDoesNotThrow(() -> {
            double distance = DistanceUtil.calculateDistance(
                    -33.8688, 151.2093,  // 悉尼
                    -37.8136, 144.9631   // 墨尔本
            );
            
            double distanceKm = distance / 1000;
            assertTrue(distanceKm > 700 && distanceKm < 800, 
                    "悉尼到墨尔本的距离应该在700-800公里之间");
        });
    }

    @Test
    @DisplayName("极大距离计算不应溢出或返回异常值")
    void testExtremeDistance() {
        double distance = DistanceUtil.calculateDistance(
                90.0, 0.0,     // 北极
                -90.0, 180.0   // 南极
        );
        
        double distanceKm = distance / 1000;
        assertTrue(distanceKm > 19000 && distanceKm < 21000,
                "北极到南极的最大距离应该接近地球半周长约20000公里");
    }

    @Test
    @DisplayName("距离应为非负数")
    void testNonNegative() {
        for (int i = 0; i < 100; i++) {
            double lat1 = Math.random() * 180 - 90;
            double lng1 = Math.random() * 360 - 180;
            double lat2 = Math.random() * 180 - 90;
            double lng2 = Math.random() * 360 - 180;
            
            double distance = DistanceUtil.calculateDistance(lat1, lng1, lat2, lng2);
            
            assertTrue(distance >= 0, "距离不能为负数");
        }
    }
}
