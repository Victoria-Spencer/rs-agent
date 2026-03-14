package org.rail.agent.pojo.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI对话响应VO
 */
@Data
public class AgentResponseVO {
    private String reply;        // AI回复话术
    private List<TicketQueryVO> ticketList; // 车票列表（独立VO）
    private Boolean canBuy;      // 是否可购票
    private Boolean needReply;   // 是否需要用户补充信息
}