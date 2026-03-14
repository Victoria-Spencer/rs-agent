package org.rail.agent.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class TicketQueryVO {
    private Train train;
    private LocalDateTime departureTime;    // 出发时间
    private LocalDateTime arrivalTime;      // 到达时间
    private Integer duration;   // 历时
    private String departure;   // 出发站点
    private String arrival;     // 到达站点
    private String departureCode;   // 出发站编码
    private String arrivalCode;     // 到达站编码
    private boolean departureFlag;  // 始发站标识
    private boolean arrivalFlag;    // 终点站标识
    private List<SeatClassFrontVO> seatClassFrontVOList;    // 席别信息
    private List<TrainTypeVO>  trainTypeVOList;     // 列车类型


    @Data
    public static class Train {
        private Long id;    // 列车id
        private String trainNumber;     // 车次
        private Integer daysArrived;    // 跨天数量
        private LocalDateTime saleTime;     // 可售时间
        private Integer saleStatus;     // 可售状态
    }

    @Data
    public static class SeatClassFrontVO {
        private Long seatClassId;   // 席别类型
        private Integer seatType;   // 席别类型
        private String name;    // 席别名称（商务座等）
        private Integer price;  // 席别价格
        private Integer availableSeatNum;   // 可用座位数量
        private Integer totalSeatNum;   // 总数量
        private boolean candidate;  // 席别候补标识
    }

    @Data
    public static class TrainTypeVO {
        private Integer typeId;     // 类型ID
        private String typeName;    // 类型名称
        private String typeCode;    // 类型编码
    }
}
