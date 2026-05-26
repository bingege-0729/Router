package com.javaee.backend.entity;



import com.javaee.backend.entity.geo.Location;
import com.javaee.backend.enums.POICategory;
import com.javaee.backend.enums.PriceLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class POI {
    // 基础信息
    private String id;
    private String name;
    private String address;
    private POICategory category;
    private List<String> subCategories;     // ["日料", "居酒屋"]

    // 地理信息
    private Location location;

    // 时间信息
    private Integer recommendedDuration;    // 建议游玩时长(分钟)
    private String openingHours;            // "10:00-18:00"
    private List<String> peakHours;         // ["12:00-14:00", "17:00-19:00"]

    // 评分与价格
    private Double rating;                  // 综合评分 1-5
    private Integer reviewCount;            // 评价数量
    private PriceLevel priceLevel;          // 价格等级
    private Double avgCost;                 // 人均消费

    // 动态信息
    private Integer currentWaitTime;        // 当前排队时间(分钟)
    private Integer currentOccupancy;       // 当前拥挤度 0-100

    // 标签与特征
    private List<String> tags;              // ["网红", "亲子", "安静"]
    private List<String> facilities;        // ["停车场", "WiFi", "母婴室"]

    // 来源
    private String dataSource;              // "dianping" / "gaode" / "meituan"
    private LocalDateTime updatedAt;
}
