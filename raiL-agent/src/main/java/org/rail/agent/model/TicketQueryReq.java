package org.rail.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

/**
 * AI 购票智能体 - 车票查询请求参数
 * 专门给大模型调用使用
 */
@Data
public class TicketQueryReq {

    @JsonProperty(required = true)
    @JsonPropertyDescription("出发站编码列表，例如：[\"XMN\"] 代表厦门")
    private List<String> departureCodes;

    @JsonProperty(required = true)
    @JsonPropertyDescription("到达站编码列表，例如：[\"SHA\"] 代表上海")
    private List<String> arrivalCodes;

    @JsonProperty(required = true)
    @JsonPropertyDescription("出发日期，格式：yyyy-MM-dd，例如：2026-05-01")
    private String departureDate;

    @JsonPropertyDescription("可选：车次类型ID，1=高铁，2=动车，3=普通")
    private List<Integer> trainTypeIds;

    @JsonPropertyDescription("可选：座位类型ID，1=商务座，2=一等座，3=二等座")
    private List<Integer> seatTypes;
}
