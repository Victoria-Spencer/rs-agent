package org.rail.agent.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

/**
 * AI 购票智能体 - 车票查询请求参数
 * 专门给大模型调用使用：接收【站点名称】，而非编码
 */
@Data
public class AiTicketQueryReq {

    @JsonProperty(required = true)
    @JsonPropertyDescription("出发站名称，例如：厦门、北京南")
    private String departureStation;

    @JsonProperty(required = true)
    @JsonPropertyDescription("到达站名称，例如：上海、杭州东")
    private String arrivalStation;

    @JsonProperty(required = true)
    @JsonPropertyDescription("出发日期，格式：yyyy-MM-dd，例如：2026-05-01")
    private String departureDate;

    @JsonPropertyDescription("【可选】车次类型ID：1=普通，2=动车，3=高铁。" +
            "⚠️ 【重要警告】除非用户在对话中明确提及'高铁'、'动车'、'普速'等具体车次类型，" +
            "否则绝对不要设置此参数！设置此参数会导致查询结果被过滤，可能查不到票！")
    private List<Integer> trainTypeIds;

    @JsonPropertyDescription("【可选】座位类型ID：1=商务座，2=一等座，3=二等座。" +
            "⚠️ 【重要警告】除非用户在对话中明确提及'商务座'、'一等座'、'二等座'等具体座位类型，" +
            "否则绝对不要设置此参数！设置此参数会导致查询结果被过滤，可能查不到票！")
    private List<Integer> seatTypes;
}