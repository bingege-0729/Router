package com.javaee.backend.entity.enrichment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SentimentScore {
    private double positive;      // 正向得分 0-1
    private double negative;      // 负向得分 0-1
    private double neutral;       // 中性得分 0-1

    public double getNetScore() {
        return positive - negative;
    }

    public String getSentiment() {
        if (positive > 0.6) return "好评";
        if (negative > 0.6) return "差评";
        return "中评";
    }

    public static SentimentScore neutral() {
        return new SentimentScore(0.33, 0.33, 0.34);
    }
}