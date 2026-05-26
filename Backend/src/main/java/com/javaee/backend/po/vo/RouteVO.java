package com.javaee.backend.po.vo;




import com.javaee.backend.entity.Route;
import com.javaee.backend.entity.geo.Bounds;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RouteVO {
    private String requestId;
    private String userId;
    private boolean success;
    private String message;

    // 主方案
    private Route mainRoute;

    // 备选方案
    private List<Route> alternatives;

    // 个性化说明
    private String personalizationNote;

    // 可视化数据
    private MapData mapData;                 // 用于前端地图渲染

    @Data
    @Builder
    public static class MapData {
        private List<LatLng> polyline;       // 路线折线
        private List<Marker> markers;        // POI标记点
        private Bounds bounds;               // 地图视野
    }

    @Data
    @Builder
    public static class Marker {
        private String poiId;
        private String name;
        private LatLng position;
        private int sequence;                // 第几个点
    }

    @Data
    @AllArgsConstructor
    public static class LatLng {
        private double lat;
        private double lng;
    }
}