package org.rail.agent.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * AI对话响应DTO
 */
@Data
public class AgentResponseDTO {
    private String reply;        // AI回复话术
    private List<TicketVO> ticketList; // 车票列表（独立VO）
    private Boolean canBuy;      // 是否可购票
    private Boolean needReply;   // 是否需要用户补充信息

    // 独立车票VO（解耦原有TicketQueryVO）
    @Data
    public static class TicketVO {
        private String trainNo;      // 车次号
        private String startTime;    // 出发时间
        private String endTime;      // 到达时间
        private String seatType;     // 坐席类型
        private String price;        // 票价
        private Integer remainNum;   // 余票数量
        private String ticketId;     // 购票ID
    }
}