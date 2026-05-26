package com.javaee.backend.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum POICategory {
    // 景点类
    SCENIC_SPOT("景点"),
    PARK("公园"),
    MUSEUM("博物馆"),
    ART_GALLERY("美术馆"),

    // 餐饮类
    RESTAURANT("餐厅"),
    CAFE("咖啡厅"),
    FOOD_COURT("美食广场"),

    // 娱乐类
    SHOPPING_MALL("商场"),
    CINEMA("电影院"),
    THEATER("剧院"),
    AMUSEMENT_PARK("游乐园"),

    // 其他
    HOTEL("酒店"),
    OTHER("其他");

    private final String displayName;

    POICategory(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return this.name();
    }

    public boolean isFoodRelated() {
        return this == RESTAURANT || this == CAFE || this == FOOD_COURT;
    }

    public boolean isEntertainment() {
        return this == CINEMA || this == THEATER || this == AMUSEMENT_PARK;
    }
}