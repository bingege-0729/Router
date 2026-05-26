package com.javaee.backend.enums;



public enum OptimizationGoal {
    SHORTEST_TIME("最短时间", "优先考虑总耗时最少"),
    LEAST_WALKING("最少步行", "优先考虑步行距离最短"),
    LOWEST_COST("最低花费", "优先考虑总花费最少"),
    LEAST_WAITING("最少排队", "优先考虑避开排队"),
    HIGHEST_RATED("最高评分", "优先考虑评分高的POI"),
    MOST_PERSONALIZED("最个性化", "优先匹配用户偏好"),
    BALANCED("均衡", "综合平衡各因素");

    private final String name;
    private final String description;

    OptimizationGoal(String name, String description) {
        this.name = name;
        this.description = description;
    }
}