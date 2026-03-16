package org.rail.agent.result;

import lombok.Data;

/**
 * LLM解析统一结果：包含意图+解析后的参数DTO
 */
@Data
public class LlmParseResult {
    /**
     * 识别出的意图（对应枚举名：QUERY_TICKET/BUY_TICKET/...）
     */
    private String intent;
    /**
     * 解析后的参数DTO（AgentTicketQueryDTO/AgentTicketBuyDTO/...）
     */
    private Object data;
    /**
     * 回话（方便前端展示）
     */
    private String reply;
}