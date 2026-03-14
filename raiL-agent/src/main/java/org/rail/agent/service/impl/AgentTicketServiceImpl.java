package org.rail.agent.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONUtil;
import org.rail.agent.dto.AgentRequestDTO;
import org.rail.agent.dto.AgentResponseDTO;
import org.rail.agent.feign.TicketServiceFeign;
import org.rail.agent.util.*;
import org.rail.common.sdk.dto.TicketQueryDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentTicketServiceImpl {
    @Resource
    private DoubaoLlmClient doubaoLlmClient;
    @Resource
    private StationCodeUtil stationCodeUtil;
    @Resource
    private FieldCheckUtil fieldCheckUtil;
    @Resource
    private DialogContextUtil dialogContextUtil;
    @Resource
    private TicketServiceFeign ticketServiceFeign;

    /**
     * 独立智能体核心逻辑：解析→校验→追问/查询
     */
    public AgentResponseDTO handleTicketQuery(AgentRequestDTO requestDTO) {
        AgentResponseDTO response = new AgentResponseDTO();
        String userInput = requestDTO.getUserInput();
        Long userId = requestDTO.getUserId();

        // 1. 加载历史上下文（独立Redis库，不依赖原有系统）
        org.rail.agent.dto.AgentTicketQueryDTO historyContext = dialogContextUtil.getContext(userId);
        org.rail.agent.dto.AgentTicketQueryDTO agentDTO;

        // 2. 解析用户输入（LLM独立调用）
        if (StrUtil.isNotEmpty(userInput)) {
            if (historyContext != null) {
                // 多轮对话：合并上下文
                agentDTO = mergeContext(historyContext, userInput);
            } else {
                // 首次对话：解析输入
                agentDTO = doubaoLlmClient.parseTicketQuery(userInput, userId);
            }
        } else {
            response.setReply("请输入你的购票需求，比如：2026年3月14日下午两点北京南到西安北买二等座");
            response.setNeedReply(true);
            response.setCanBuy(false);
            return response;
        }

        // 3. 字段校验+转换为原有系统的TicketQueryDTO
        FieldCheckUtil.CheckResult checkResult = fieldCheckUtil.checkAndConvert(agentDTO, stationCodeUtil);
        if (checkResult.isMissing()) {
            // 字段缺失：保存上下文+返回追问
            dialogContextUtil.saveContext(userId, agentDTO);
            response.setReply(checkResult.getAskMessage());
            response.setNeedReply(true);
            response.setCanBuy(false);
            return response;
        }

        // 4. 字段完整：调用原有ticket-service查询
        try {
            TicketQueryDTO ticketQueryDTO = checkResult.getTicketQueryDTO();
            List<org.rail.common.sdk.dto.TicketQueryVO> ticketVOList = ticketServiceFeign.queryTicket(ticketQueryDTO);

            // 转换为独立VO（解耦原有VO）
            List<AgentResponseDTO.TicketVO> ticketList = ticketVOList.stream().map(vo -> {
                AgentResponseDTO.TicketVO ticketVO = new AgentResponseDTO.TicketVO();
                ticketVO.setTrainNo(vo.getTrainNo());
                ticketVO.setStartTime(LocalDateTimeUtil.format(vo.getStartTime(), "yyyy-MM-dd HH:mm"));
                ticketVO.setEndTime(LocalDateTimeUtil.format(vo.getEndTime(), "yyyy-MM-dd HH:mm"));
                ticketVO.setSeatType(vo.getSeatType());
                ticketVO.setPrice(vo.getPrice().toString());
                ticketVO.setRemainNum(vo.getRemainNum());
                ticketVO.setTicketId(vo.getTicketId());
                return ticketVO;
            }).collect(Collectors.toList());

            // 5. 整理响应
            if (ticketList.isEmpty()) {
                response.setReply(StrUtil.format("抱歉，{}从{}到{}暂无可用车票",
                        agentDTO.getTravelTime().toLocalDate(),
                        agentDTO.getStartStation(),
                        agentDTO.getEndStation()));
                response.setCanBuy(false);
            } else {
                StringBuilder reply = new StringBuilder(StrUtil.format("为你找到以下{}合适的车次：\n", agentDTO.getSeatType()));
                for (int i = 0; i < Math.min(ticketList.size(), 3); i++) {
                    AgentResponseDTO.TicketVO ticket = ticketList.get(i);
                    reply.append(StrUtil.format("{}. 车次{}：{}出发→{}到达，{}，票价{}元，余票{}张\n",
                            i+1, ticket.getTrainNo(),
                            ticket.getStartTime(), ticket.getEndTime(),
                            ticket.getSeatType(), ticket.getPrice(), ticket.getRemainNum()));
                }
                reply.append("点击车次即可购票～");
                response.setReply(reply.toString());
                response.setTicketList(ticketList);
                response.setCanBuy(true);
            }
            response.setNeedReply(false);
            // 清除上下文
            dialogContextUtil.clearContext(userId);
        } catch (Exception e) {
            response.setReply("查询车票失败，请稍后重试：" + e.getMessage());
            response.setCanBuy(false);
            response.setNeedReply(false);
        }
        return response;
    }

    /**
     * 合并历史上下文与新输入（独立LLM处理）
     */
    private org.rail.agent.dto.AgentTicketQueryDTO mergeContext(
            org.rail.agent.dto.AgentTicketQueryDTO history, String userInput) {
        String prompt = StrUtil.format("""
                已知用户之前的购票需求：{}
                现在用户补充信息：{}
                请整合所有信息，返回完整的购票信息JSON（格式：{"startStation":"","endStation":"","travelTime":"yyyy-MM-dd HH:mm","seatType":""}）
                """, JSONUtil.toJsonStr(history), userInput);
        return doubaoLlmClient.parseTicketQuery(prompt, history.getUserId());
    }
}