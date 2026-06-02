package com.javaee.backend.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.javaee.backend.entity.enrichment.SentimentScore;

import com.javaee.backend.enums.POICategory;
import com.javaee.backend.enums.PriceLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@TableName("poi")
public class POI {
    // 基础信息
    @TableId
    private String id;
    private String name;
    private String address;
    private POICategory category;
    private String subCategory;             // JSON 或 VARCHAR
    private String city;
    private String district;
    private Integer status;
    // 地理信息
    private BigDecimal lat;
    private BigDecimal lng;

    // 时间信息
    private Integer recommendedDuration;    // 建议游玩时长(分钟)
    private String openingHours;            // "10:00-18:00"
    @TableField("peak_hours")
    private String peakHours;               // JSON 或 VARCHAR

    // 评分与价格
    private Double rating;                  // 综合评分 1-5
    private Integer reviewCount;            // 评价数量
    private PriceLevel priceLevel;          // 价格等级
    private Double avgCost;                 // 人均消费

    // 动态信息
    @TableField("avg_wait_time")
    private Integer currentWaitTime;        // 当前排队时间(分钟)
    @TableField("current_crowd_level")
    private Integer currentOccupancy;       // 当前拥挤度 0-100
    @TableField("crowd_updated_at")
    private LocalDateTime crowdUpdatedAt;

    // 标签与特征
    @TableField(value = "tags", typeHandler = com.javaee.backend.config.StringListTypeHandler.class)
    private List<String> tags = new ArrayList<>();              // ["网红", "亲子", "安静"]
    @TableField(value = "facilities", typeHandler = com.javaee.backend.config.StringListTypeHandler.class)
    private List<String> facilities = new ArrayList<>();        // ["停车场", "WiFi", "母婴室"]

    // 来源
    private String dataSource;              // "dianping" / "gaode" / "meituan"
    @TableField("created_at")
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Review> reviews;           // 评价列表
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SentimentScore sentimentScore;  // 情感分析得分
    
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String description;             // LLM生成的亮点描述（不持久化）

}
