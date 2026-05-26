package com.javaee.backend.controller;

import com.javaee.backend.common.Result;
import com.javaee.backend.entity.POI;
import com.javaee.backend.service.POIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/poi")
public class POIController {

    @Autowired
    private POIService poiService;

    /**
     * 获取所有POI
     * @return POI列表
     */
    @GetMapping("/list")
    public Result<List<POI>> list() {
        List<POI> pois = poiService.getAll();
        return Result.success(pois);
    }

    /**
     * 获取POI详情
     * @param id        POI id
     * @return          POI
     */
    @GetMapping("/{id}")
    public Result<POI> getById(String id) {
        return Result.success(poiService.getById(id));
    }
    /**
     * 获取指定类别的POI
     * @param category  类别
     * @return          POI列表
     */
    @GetMapping("/category/{category}")
    public Result<List<POI>> getByCategory(String category) {
        List<POI> pois = poiService.getByCategory(category);
        return Result.success(pois);
    }

    /**
     * 搜索附近POI
     * @param lat           经度
     * @param lng           纬度
     * @param category      类别
     * @param radius        半径
     * @param limit         限制数量
     * @return              POI列表
     */
    @GetMapping("/nearby")
    public Result<List<POI>> searchNearby(BigDecimal lat, BigDecimal lng, String category, Integer radius, Integer limit) {
        List<POI> pois = poiService.searchNearby(lat, lng, category, radius, limit);
        return Result.success(pois);
    }

    /**
     * 获取热门POI
     * @param limit 限制数量
     * @return POI列表
     */
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> getHotPOIs(Integer limit) {
        List<Map<String, Object>> pois = poiService.getHotPOIs(limit);
        return Result.success(pois);
    }
}
