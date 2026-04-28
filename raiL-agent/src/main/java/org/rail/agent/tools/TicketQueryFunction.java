package org.rail.agent.tools;

import lombok.extern.slf4j.Slf4j;
import org.apache.juli.LogUtil;
import org.rail.agent.model.api.station.StationPageQueryApiReq;
import org.rail.agent.model.api.station.StationQueryResp;
import org.rail.agent.model.api.ticket.TicketQueryApiReq;
import org.rail.agent.model.ai.AiTicketQueryReq;
import org.rail.agent.model.api.ticket.TicketQueryResp;
import org.rail.agent.result.PageResult;
import org.rail.agent.result.Result;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

@Component
@Slf4j
public class TicketQueryFunction implements BiFunction<AiTicketQueryReq, ToolContext, String> {

    private static final String TICKET_QUERY_URL = "http://localhost:8080/api/ticket-service/ticket/query";
    private static final String STATION_QUERY_URL = "http://localhost:8080/api/ticket-service/stations/page";

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String apply(AiTicketQueryReq req, ToolContext toolContext) {
        try {
            // 1. 自动把站点名称转编码
            List<String> depCodes = getStationCode(req.getDepartureStation());
            List<String> arrCodes = getStationCode(req.getArrivalStation());

            if (depCodes.isEmpty()) {
                return "❌ 出发站【" + req.getDepartureStation() + "】不存在，请检查名称";
            }
            if (arrCodes.isEmpty()) {
                return "❌ 到达站【" + req.getArrivalStation() + "】不存在，请检查名称";
            }

            // 2. 构造接口请求
            TicketQueryApiReq apiReq = new TicketQueryApiReq();
            apiReq.setDepartureCodes(depCodes);
            apiReq.setArrivalCodes(arrCodes);
            apiReq.setDepartureDate(LocalDate.parse(req.getDepartureDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            apiReq.setTrainTypeIds(req.getTrainTypeIds());
            apiReq.setSeatTypes(req.getSeatTypes());

            // 3. 调用车票接口
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TicketQueryApiReq> request = new HttpEntity<>(apiReq, headers);

            /*ResponseEntity<String> rawResponse = restTemplate.exchange(
                    TICKET_QUERY_URL, HttpMethod.POST, request, String.class
            );
            System.out.println("=== 后端接口原始返回 ===" + rawResponse.getBody());*/

            // 关键：用 ParameterizedTypeReference 解析泛型，无需 TicketQueryResult
            ResponseEntity<Result<List<TicketQueryResp>>> response = restTemplate.exchange(
                    TICKET_QUERY_URL,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Result<List<TicketQueryResp>>>() {}
            );

            // 4. 格式化结果
            if (response.getBody() != null && response.getBody().getData() != null && !response.getBody().getData().isEmpty()) {
                return formatTicketResult(response.getBody().getData());
            }
            return "❌ 未查询到车票信息";
        } catch (Exception e) {
            log.error("车票查询接口调用失败", e);
            return "❌ 查询失败：" + e.getMessage();
        }
    }

    // 根据站点名自动查编码
    private List<String> getStationCode(String stationName) {
        try {
            StationPageQueryApiReq req = new StationPageQueryApiReq();
            req.setKeyword(stationName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<StationPageQueryApiReq> request = new HttpEntity<>(req, headers);

            ResponseEntity<Result<PageResult<StationQueryResp>>> response = restTemplate.exchange(
                    STATION_QUERY_URL,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Result<PageResult<StationQueryResp>>>() {}
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData().getRecords().stream()
                        .map(StationQueryResp::getCode)
                        .toList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    private String formatTicketResult(List<TicketQueryResp> tickets) {
        StringBuilder sb = new StringBuilder("✅ 车票查询结果：\n");
        for (TicketQueryResp ticket : tickets) {
            sb.append(String.format("【%s】 %s → %s%n", ticket.getTrain().getTrainNumber(), ticket.getDeparture(), ticket.getArrival()));
            sb.append(String.format(" 出发：%s | 到达：%s | 历时：%d分钟%n", ticket.getDepartureTime(), ticket.getArrivalTime(), ticket.getDuration()));
            sb.append("----------------------------------------%n");
        }
        return sb.toString();
    }
}