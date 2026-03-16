package org.rail.agent.controller;

import jakarta.annotation.Resource;
import org.rail.agent.pojo.dto.AgentRequestDTO;
import org.rail.agent.pojo.dto.AgentResponseVO;
import org.rail.agent.service.impl.AgentServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 独立智能体对外API
 */
@RestController
@RequestMapping("/agent")
public class  AgentController {
    @Resource
    private AgentServiceImpl agentService;

    /**
     * AI购票对话接口（独立入口）
     */
    @PostMapping("/chat")
    public AgentResponseVO chat(@RequestBody AgentRequestDTO requestDTO) {
        return agentService.chat(requestDTO);
    }
}