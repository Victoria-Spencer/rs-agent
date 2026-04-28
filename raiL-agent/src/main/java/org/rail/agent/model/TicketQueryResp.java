package org.rail.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 远程车票服务接口 - 查询响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketQueryResp {

    // 列车信息
    private Train train;

    // 出发到达时间
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    // 历时(分钟)
    private Integer duration;

    // 站点信息
    private String departure;
    private String arrival;
    private String departureCode;
    private String arrivalCode;

    // 标识
    private boolean departureFlag;
    private boolean arrivalFlag;

    // 座位、车型列表
    private List<SeatClassFrontVO> seatClassFrontVOList;
    private List<TrainTypeVO> trainTypeVOList;
}