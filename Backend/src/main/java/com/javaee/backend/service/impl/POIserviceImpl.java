package com.javaee.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaee.backend.entity.POI;
import com.javaee.backend.mapper.POIMapper;
import com.javaee.backend.service.POIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 获取POI信息
     * @param id POI id
     * @return POI
     */
    @Override
    public POI getPOIById(String id) {
        log.info("查询POI:id={}",id);
        return POIMapper.selectById(id);
    }

    @Override
    public List<POI> getAll() {
        log.info("查询所有POI");
        LambdaQueryWrapper<POI> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(POI::getStatus,1);
        wrapper.orderByDesc(POI::getRating);
        return POIMapper.selectList(wrapper);
    }

    /**
     * 根据类别获取POI信息
     * @param category 类别
     * @return POI列表
     */
    @Override
    public List<POI> getByCategory(String category) {
        log.info("查询类别为{}的POI",category);
        return POIMapper.findByCategory(category);
    }

    /**
     * 根据经纬度搜索附近POI
     * @param lat 纬度
     * @param lng 经度
     * @param category 类别
     * @param radius 半径
     * @param limit 限制数量
     * @return POI列表
     */
    @Override
    public List<POI> searchNearby(BigDecimal lat, BigDecimal lng, String category, Integer radius, Integer limit) {
        log.info("查询附近POI:lat={},lng={},category={},radius={},limit={}",lat,lng,category,radius,limit);
        return POIMapper.searchNearby(lat,lng,radius,category,limit);
    }

    /**
     * 获取热门POI
     * @param limit 限制数量
     * @return POI列表
     */
    @Override
    public List<Map<String, Object>> getHotPOIs(Integer limit) {
        log.info("查询热门POI:limit={}",limit);
        return POIMapper.getHotPOIs(limit);
    }
}
