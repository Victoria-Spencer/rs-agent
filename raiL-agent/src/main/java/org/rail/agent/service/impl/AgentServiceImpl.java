package org.rail.agent.service.impl;

import org.rail.agent.pojo.dto.AgentRequestDTO;
import org.rail.agent.pojo.dto.AgentResponseVO;
import org.springframework.stereotype.Service;

@Service
public class AgentServiceImpl implements AgentService {

    /**
     * 独立智能体核心逻辑：识别意图、解析→校验→追问/查询
     */
    public AgentResponseVO chat(AgentRequestDTO requestDTO) {
        try {
            // 1. 调用工具类：自动识别意图+解析参数（仅需传入用户输入）
            LlmParseResult parseResult = llmParseUtil.autoParse(requestDTO.getUserInput());

            // 2. 根据识别的意图，路由到对应业务逻辑
            switch (parseResult.getIntent()) {
                case "QUERY_TICKET":
                    /*// 解析为查询DTO，调用查询业务
                    AgentTicketQueryDTO queryDTO = (AgentTicketQueryDTO) parseResult.getData();
                    // TODO: 调用车票查询接口，获取ticketList
                    // List<AgentResponseVO.TicketVO> ticketList = ticketQueryService.query(queryDTO);
                    // response.setTicketList(ticketList);
                    response.setReply("已为你查询到车票信息：" + queryDTO.getStartStation() + "→" + queryDTO.getEndStation());
                    response.setCanBuy(true);
                    response.setNeedReply(false);
                    break;*/
                case "BUY_TICKET":
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            /*response.setReply("解析失败：" + e.getMessage());
            response.setNeedReply(true);*/
            e.printStackTrace();
        }
    }
}