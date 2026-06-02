package com.javaee.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.javaee.backend.entity.POI;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface POIService extends IService<POI> {

    POI getPOIById(String id);

    POI getByName(String name);

    List<POI> getAll();

    List<POI> getByCategory(String category);

    List<POI> searchNearby(BigDecimal lat,
                           BigDecimal lng,
                           String category,
                           Integer radius,
                           Integer limit
                           );

    List<Map<String,Object>> getHotPOIs(Integer limit);



}
