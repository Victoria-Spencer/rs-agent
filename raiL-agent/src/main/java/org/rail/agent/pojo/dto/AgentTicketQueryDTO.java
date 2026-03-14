package org.rail.agent.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LLM解析后的购票查询DTO
 */
@Data
public class AgentTicketQueryDTO {
    private Long userId;            // 用户ID
    private String startStation;    // 出发站名称
    private String endStation;      // 到达站名称
    private LocalDateTime travelTime; // 出行时间
    private String seatType = "二等座"; // 坐席类型（默认二等座）
    // 扩展字段（与原有系统对齐）
    private List<Integer> trainTypeIds; // 列车类型ID
}