package org.rail.agent.controller;

import jakarta.annotation.Resource;
import org.rail.agent.dto.AgentRequestDTO;
import org.rail.agent.dto.AgentResponseDTO;
import org.rail.agent.service.impl.AgentTicketServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 独立智能体对外API
 */
@RestController
@RequestMapping("/ticket")
public class  AgentTicketController {
    @Resource
    private AgentTicketServiceImpl agentTicketService;

    /**
     * AI购票对话接口（独立入口）
     */
    @PostMapping("/chat")
    public AgentResponseDTO chat(@RequestBody AgentRequestDTO requestDTO) {
        return agentTicketService.handleTicketQuery(requestDTO);
    }
}