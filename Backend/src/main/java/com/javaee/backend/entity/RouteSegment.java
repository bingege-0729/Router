package com.javaee.backend.entity;




import com.javaee.backend.enums.TransportMode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RouteSegment {
    private Integer sequence;                 // 顺序 1,2,3...

    // POI信息
    private POI poi;
    private LocalDateTime arriveTime;
    private LocalDateTime leaveTime;
    private Integer stayDuration;             // 停留时长(分钟)

    // 交通信息(上一站到本站)
    private TransportMode transportMode;      // 步行/打车/公交
    private Integer travelDuration;           // 交通耗时(分钟)
    private Integer travelDistance;           // 交通距离(米)

    // 排队信息
    private Integer expectedWaitTime;         // 预计排队时间
    private String waitTimeTip;               // "建议错峰，12点后人少"

    // 实用信息
    private String entryTip;                  // "西门进不用排队"
    private String parkingInfo;               // "附近有停车场，10元/小时"
    private List<String> nearbyAlternatives;  // 附近的备选POI
}