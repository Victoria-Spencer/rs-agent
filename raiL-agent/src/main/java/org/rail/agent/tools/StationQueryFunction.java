package org.rail.agent.tools;

import lombok.Data;
import org.rail.agent.model.ai.AiStationQueryReq;
import org.rail.agent.model.api.station.StationPageQueryApiReq;
import org.rail.agent.model.api.station.StationQueryResp;
import org.rail.agent.result.PageResult;
import org.rail.agent.result.Result;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.function.BiFunction;

@Component
public class StationQueryFunction implements BiFunction<AiStationQueryReq, ToolContext, String> {

    private static final String STATION_QUERY_URL = "http://localhost:8080/api/ticket-service/stations/page";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String apply(AiStationQueryReq req, ToolContext toolContext) {
        try {
            // 构造查询DTO
            StationPageQueryApiReq apiReq = new StationPageQueryApiReq();
            apiReq.setKeyword(req.getKeyword());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<StationPageQueryApiReq> request = new HttpEntity<>(apiReq, headers);

            // 调用远程接口
            ResponseEntity<Result<PageResult<StationQueryResp>>> response = restTemplate.exchange(
                    STATION_QUERY_URL,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Result<PageResult<StationQueryResp>>>() {}
            );

            // 解析结果
            if (response.getBody() != null && response.getBody().getData() != null) {
                var records = response.getBody().getData().getRecords();
                if (!records.isEmpty()) {
                    StationQueryResp station = records.get(0);
                    return "✅ 站点查询成功：%s，编码：%s".formatted(station.getName(), station.getCode());
                }
            }
            return "❌ 未找到对应站点";
        } catch (Exception e) {
            return "❌ 站点查询失败：" + e.getMessage();
        }
    }
}