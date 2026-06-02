package com.javaee.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaee.backend.entity.POI;
import com.javaee.backend.mapper.POIMapper;
import com.javaee.backend.service.POIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class POIserviceImpl extends ServiceImpl<POIMapper, POI> implements POIService {
    @Autowired
    private POIMapper POIMapper;


    /**
     * 获取POI信息（带缓存）
     * 缓存key: poi:id:{id}
     */
    @Override
    @Cacheable(value = "poi", key = "'poi:id:' + #id", unless = "#result == null")
    public POI getPOIById(String id) {
        log.info("🔍 [DB查询] 查询POI: id={}", id);
        return POIMapper.selectById(id);
    }

    @Override
    public POI getByName(String name) {
        log.info("🔍 [DB查询] 按名称查询POI: name={}", name);
        LambdaQueryWrapper<POI> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(POI::getName, name);
        return POIMapper.selectOne(wrapper);
    }

    @Override
    public List<POI> getAll() {
        log.info("🔍 [DB查询] 查询所有POI");
        LambdaQueryWrapper<POI> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(POI::getStatus, 1);
        wrapper.orderByDesc(POI::getRating);
        return POIMapper.selectList(wrapper);
    }

    /**
     * 根据类别获取POI信息（带缓存）
     * 缓存key: poi:category:{category}
     */
    @Override
    @Cacheable(value = "poi", key = "'poi:category:' + #category", unless = "#result == null || #result.isEmpty()")
    public List<POI> getByCategory(String category) {
        log.info("🔍 [DB查询] 查询类别为{}的POI", category);
        return POIMapper.findByCategory(category);
    }

    /**
     * 根据经纬度搜索附近POI（带缓存）
     * 缓存key: poi:nearby:{lat}:{lng}:{category}
     */
    @Override
    @Cacheable(value = "poi", key = "'poi:nearby:' + #lat + ':' + #lng + ':' + (#category != null ? #category : 'all')", unless = "#result == null || #result.isEmpty()")
    public List<POI> searchNearby(BigDecimal lat, BigDecimal lng, String category, Integer radius, Integer limit) {
        log.info("🔍 [DB查询] 查询附近POI: lat={}, lng={}, category={}", lat, lng, category);
        return POIMapper.searchNearby(lat, lng, radius, category, limit);
    }

    /**
     * 获取热门POI（带缓存）
     * 缓存key: poi:hot:{limit}
     */
    @Override
    @Cacheable(value = "poi", key = "'poi:hot:' + #limit", unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getHotPOIs(Integer limit) {
        log.info("🔍 [DB查询] 查询热门POI: limit={}", limit);
        return POIMapper.getHotPOIs(limit);
    }
}
