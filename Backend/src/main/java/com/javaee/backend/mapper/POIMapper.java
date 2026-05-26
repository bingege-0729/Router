package com.javaee.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.backend.entity.POI;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface POIMapper extends BaseMapper<POI> {
    /**
     * 根据类别查找POI
     * @param category 类别
     * @return
     */
    List<POI> findByCategory(@Param("category") String category);

    /**
     * 根据经纬度查找附近POI
     * @param lat
     * @param lng
     * @param radius
     * @param category
     * @param limit
     * @return
     */
    List<POI> searchNearby(@Param("latitude")BigDecimal lat,
                           @Param("longitude")BigDecimal lng,
                           @Param("radius")Integer radius,
                           @Param("category")String category,
                           @Param("limit")Integer limit
                           );

    /**
     * 获取热门POI
     * @param limit
     * @return
     */

    @Select("SELECT * FROM poi ORDER BY likes DESC LIMIT #{limit}")
    List<Map<String, Object>> getHotPOIs(@Param("limit")Integer  limit);

}
