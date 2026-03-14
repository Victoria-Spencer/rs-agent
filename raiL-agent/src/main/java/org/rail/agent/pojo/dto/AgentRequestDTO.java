package org.rail.agent.pojo.dto;

import lombok.Data;

/**
 * 前端调用AI对话的请求DTO
 */
@Data
public class AgentRequestDTO {
    private String userInput; // 用户自然语言输入
//    private Long userId;      // 用户ID（关联原有系统用户）
}