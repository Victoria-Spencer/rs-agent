package org.rail.agent;

import org.rail.agent.model.*;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.function.BiFunction;

@Component
public class StationQueryFunction implements BiFunction<StationQueryReq, ToolContext, String> {

    private static final String STATION_QUERY_URL = "http://localhost:8080/api/ticket-service/stations/page";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String apply(StationQueryReq req, ToolContext toolContext) {
        try {
            // 构造查询DTO
            StationPageQueryApiReq apiReq = new StationPageQueryApiReq();
            apiReq.setKeyword(req.getKeyword());
            apiReq.setPageNum(1);
            apiReq.setPageSize(10);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<StationPageQueryApiReq> request = new HttpEntity<>(apiReq, headers);

            // 调用远程接口
            ResponseEntity<Result<PageResult<StationQueryResp>>> response = restTemplate.postForEntity(
                    STATION_QUERY_URL, request,
                    (Class<Result<PageResult<StationQueryResp>>>) (Class<?>) Result.class
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

    // 站点接口请求对象
    public static class StationPageQueryApiReq {
        private Integer queryType;
        private String keyword;
        private long pageNum = 1;
        private long pageSize = 10;

        public Integer getQueryType() {return queryType;}
        public void setQueryType(Integer queryType) {this.queryType = queryType;}
        public String getKeyword() {return keyword;}
        public void setKeyword(String keyword) {this.keyword = keyword;}
        public long getPageNum() {return pageNum;}
        public void setPageNum(long pageNum) {this.pageNum = pageNum;}
        public long getPageSize() {return pageSize;}
        public void setPageSize(long pageSize) {this.pageSize = pageSize;}
    }
}