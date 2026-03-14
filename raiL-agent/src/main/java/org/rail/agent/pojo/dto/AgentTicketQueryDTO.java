package org.rail.agent.pojo.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * LLM解析后的购票查询DTO
 */
@Data
public class AgentTicketQueryDTO {
    // ============= 待实现 ================
        /*private String trainNo;      // 车次号
        private String startTime;    // 出发时间
        private String endTime;      // 到达时间
        private String price;        // 票价*/

    // TODO 解析出startStation,endStation后，调用station得到对应编码
    private String startStation;     // 出发站名称
    private String endStation;      // 到达站名称
    private List<Integer> trainTypeIds;     // 列车类型ID（可选）
    private List<Integer> seatTypes;    // 席别类型（可选）
    private LocalDate travelDate; // 出行日
}