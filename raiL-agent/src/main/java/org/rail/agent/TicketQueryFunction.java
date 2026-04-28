package org.rail.agent;

import lombok.Data;
import org.rail.agent.model.*;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiFunction;

@Component
public class TicketQueryFunction implements BiFunction<TicketQueryReq, ToolContext, String> {

    private static final String TICKET_QUERY_URL = "http://localhost:8080/api/ticket-service/ticket/query";
    private static final String STATION_QUERY_URL = "http://localhost:8080/api/ticket-service/stations/page";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String apply(TicketQueryReq req, ToolContext toolContext) {
        try {
            // 1. 自动把站点名称转编码（核心！）
            String depCode = getStationCode(req.getDepartureCodes().get(0));
            String arrCode = getStationCode(req.getArrivalCodes().get(0));

            // 2. 构造接口请求
            TicketQueryApiReq apiReq = new TicketQueryApiReq();
            apiReq.setDepartureCodes(List.of(depCode));
            apiReq.setArrivalCodes(List.of(arrCode));
            apiReq.setDepartureDate(LocalDate.parse(req.getDepartureDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            apiReq.setTrainTypeIds(req.getTrainTypeIds());
            apiReq.setSeatTypes(req.getSeatTypes());

            // 3. 调用车票接口
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TicketQueryApiReq> request = new HttpEntity<>(apiReq, headers);
            ResponseEntity<Result<TicketQueryResult>> response = restTemplate.postForEntity(
                    TICKET_QUERY_URL, request,
                    (Class<Result<TicketQueryResult>>) (Class<?>) Result.class
            );

            // 4. 格式化结果
            if (response.getBody() != null && response.getBody().getData() != null && !response.getBody().getData().getData().isEmpty()) {
                return formatTicketResult(response.getBody().getData().getData());
            }
            return "❌ 未查询到车票信息";
        } catch (Exception e) {
            return "❌ 查询失败：" + e.getMessage();
        }
    }

    // 🔥 核心：根据站点名自动查编码
    private String getStationCode(String stationName) {
        try {
            StationQueryFunction.StationPageQueryApiReq req = new StationQueryFunction.StationPageQueryApiReq();
            req.setKeyword(stationName);
            req.setPageSize(1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<StationQueryFunction.StationPageQueryApiReq> request = new HttpEntity<>(req, headers);

            ResponseEntity<Result<PageResult<StationQueryResp>>> response = restTemplate.postForEntity(
                    STATION_QUERY_URL, request,
                    (Class<Result<PageResult<StationQueryResp>>>) (Class<?>) Result.class
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                var list = response.getBody().getData().getRecords();
                if (!list.isEmpty()) return list.get(0).getCode();
            }
        } catch (Exception ignored) {}
        return stationName;
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

    @Data
    public static class TicketQueryResult {
        private List<TicketQueryResp> data;
    }
}