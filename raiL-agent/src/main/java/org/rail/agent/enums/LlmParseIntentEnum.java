package org.rail.agent.enums;

import org.rail.agent.pojo.dto.AgentTicketBuyDTO;
import org.rail.agent.pojo.dto.AgentTicketQueryDTO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LLM解析意图枚举（支持自动识别）
 */
public enum LlmParseIntentEnum {
    /**
     * 车票查询
     */
    QUERY_TICKET(
            "车票查询",
            // 参数解析模板（识别意图后，用此模板二次解析参数）
            "你是12306智能购票助手，仅解析【车票查询】指令为JSON，无需额外解释！" +
                    "JSON格式要求：{\"startStation\":\"出发站全称\",\"endStation\":\"到达站全称\",\"travelTime\":\"yyyy-MM\",\"seatType\":\"席别（如二等座）\"}" +
                    "注意：1.travelTime必须是yyyy-MM格式；2.无的字段留空字符串，不要省略",
            AgentTicketQueryDTO.class
    ),

    /**
     * 车票购买
     */
    BUY_TICKET(
            "车票购买",
            // 参数解析模板
            "你是12306智能购票助手，仅解析【车票购买】指令为JSON，无需额外解释！" +
                    "JSON格式要求：{\"startStation\":\"出发站全称\",\"endStation\":\"到达站全称\",\"travelTime\":\"yyyy-MM-dd HH:mm\",\"seatType\":\"席别（如二等座）\",\"userId\":\"用户ID\",\"ticketNum\":\"购票数量\",\"trainNo\":\"车次号\"}" +
                    "注意：1.travelTime必须是yyyy-MM-dd HH:mm格式；2.无的字段留空字符串，不要省略",
            AgentTicketBuyDTO.class
    );

    // 意图描述
    private final String intentDesc;
    // 参数解析模板（识别意图后，用此模板解析参数）
    private final String paramParseTemplate;
    // 目标DTO类型
    private final Class<?> targetDtoClass;

    LlmParseIntentEnum(String intentDesc, String paramParseTemplate, Class<?> targetDtoClass) {
        this.intentDesc = intentDesc;
        this.paramParseTemplate = paramParseTemplate;
        this.targetDtoClass = targetDtoClass;
    }

    // ========== 新增：动态获取所有枚举意图名称（如QUERY_TICKET、BUY_TICKET） ==========
    public static List<String> getAllIntentNames() {
        return Arrays.stream(values())
                .map(Enum::name) // 获取枚举名（QUERY_TICKET/BUY_TICKET）
                .collect(Collectors.toList());
    }

    // ========== 改造：意图识别指令模板（动态插入枚举意图） ==========
    /**
     * 意图识别专属指令：动态传入枚举意图列表，让AI仅从列表中选择
     */
    public static String getIntentRecognizeTemplate() {
        // 拼接枚举所有意图名称（如["QUERY_TICKET","BUY_TICKET"]）
        String intentList = String.join(",", getAllIntentNames());
        return "你是12306智能意图识别助手，仅完成2件事：" +
                "1. 识别用户输入的核心意图，仅能从以下列表中选择：[" + intentList + "]；若完全无匹配，意图填\"OTHER\"；" +
                "2. 提取输入中的所有相关参数（出发站、到达站、出行时间、席别、用户ID、购票数量、车次号等）；" +
                "输出JSON格式：{\"intent\":\"识别的意图（仅列表中的值或OTHER）\",\"params\":{参数键值对}}，无需额外解释！" +
                "注意：params中无的参数留空字符串，不要省略字段。";
    }

    // Getter
    public String getIntentDesc() {
        return intentDesc;
    }

    public String getParamParseTemplate() {
        return paramParseTemplate;
    }

    public Class<?> getTargetDtoClass() {
        return targetDtoClass;
    }

    // 根据意图字符串匹配枚举（核心：自动识别后匹配）
    public static LlmParseIntentEnum getByIntentStr(String intentStr) {
        for (LlmParseIntentEnum enumItem : values()) {
            if (enumItem.name().equals(intentStr)) {
                return enumItem;
            }
        }
        // 无匹配枚举时返回null（不再直接抛异常，交给工具类处理）
        return null;
    }
}