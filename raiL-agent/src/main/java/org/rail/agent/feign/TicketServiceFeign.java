package org.rail.agent.feign;

import org.rail.agent.dto.TicketQueryDTO; // 复用SDK中的DTO
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 调用原有ticket-service的Feign客户端
 * name：原有ticket-service的服务名（注册到Nacos的名称）
 */
@FeignClient(name = "${rail.service.ticket}")
public interface TicketServiceFeign {
    /**
     * 调用原有车票查询接口
     */
    @PostMapping("/ticket/query")
    List<org.rail.common.sdk.dto.TicketQueryVO> queryTicket(@RequestBody TicketQueryDTO ticketQueryDTO);
}