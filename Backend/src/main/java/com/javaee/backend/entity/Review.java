package com.javaee.backend.entity;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    private String reviewId;
    private String poiId;
    private String userId;
    private String userName;

    // 评价内容
    private String content;                      // 原文
    private Integer rating;                      // 1-5分
    private List<String> images;                 // 图片URL

    // 标签(用户打的标签)
    private List<String> tags;                   // ["服务好", "环境不错"]

    // 结构化信息
    private Integer waitTime;                    // 用户反馈的排队时间
    private Double costPerPerson;                // 人均消费
    private String visitTime;                    // "周末下午"

    // 元信息
    private Integer usefulCount;                 // 有用数
    private LocalDateTime createdAt;

    // 是否优质评价
    private Boolean isHighQuality;               // 超过100字或有用>10
}