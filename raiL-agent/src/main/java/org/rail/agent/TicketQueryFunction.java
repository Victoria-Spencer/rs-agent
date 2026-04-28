package org.rail.agent;

import org.rail.agent.model.TicketQueryApiReq;
import org.rail.agent.model.TicketQueryReq;
import org.rail.agent.model.TicketQueryResp;
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

/**
 * AI 智能体专用工具：调用远程 ticket-service 查询车票
 * 适配 org.rail.agent.model 包下的所有模型
 */
@Component
public class TicketQueryFunction implements BiFunction<TicketQueryReq, ToolContext, String> {

    // 远程车票查询接口地址（和你controller的路径一致）
    private static final String TICKET_QUERY_URL = "http://localhost:8080/api/ticket-service/ticket/query";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String apply(TicketQueryReq req, ToolContext toolContext) {
        try {
            // 1. 把AI传的参数，转换成接口要求的 TicketQueryApiReq
            TicketQueryApiReq apiReq = convertToApiReq(req);

            // 2. 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. 发送请求调用接口
            HttpEntity<TicketQueryApiReq> request = new HttpEntity<>(apiReq, headers);
            ResponseEntity<TicketQueryResult> response = restTemplate.postForEntity(TICKET_QUERY_URL, request, TicketQueryResult.class);

            // 4. 格式化返回结果，让AI能清晰地回复给用户
            if (response.getBody() != null && response.getBody().getData() != null && !response.getBody().getData().isEmpty()) {
                return formatTicketResult(response.getBody().getData());
            }
            return "❌ 未查询到对应车票信息，请检查出发地、目的地或日期是否正确";
        } catch (Exception e) {
            return "❌ 查询失败：" + e.getMessage();
        }
    }

    /**
     * 把AI解析的TicketQueryReq，转换成接口要求的TicketQueryApiReq
     */
    private TicketQueryApiReq convertToApiReq(TicketQueryReq req) {
        TicketQueryApiReq apiReq = new TicketQueryApiReq();

        // 1. 处理必填的站点编码列表
        apiReq.setDepartureCodes(req.getDepartureCodes());
        apiReq.setArrivalCodes(req.getArrivalCodes());

        // 2. 处理日期：把用户输入的字符串（yyyy-MM-dd）转成LocalDate
        if (req.getDepartureDate() != null) {
            apiReq.setDepartureDate(LocalDate.parse(req.getDepartureDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        // 3. 处理可选参数（车次类型、座位类型）
        apiReq.setTrainTypeIds(req.getTrainTypeIds());
        apiReq.setSeatTypes(req.getSeatTypes());

        return apiReq;
    }

    /**
     * 格式化返回的车票数据，让AI能更友好地回复给用户
     * 适配你 model.Train 类的字段名 trainNumber
     */
    private String formatTicketResult(List<TicketQueryResp> tickets) {
        StringBuilder sb = new StringBuilder("✅ 车票查询结果：\n");
        for (TicketQueryResp ticket : tickets) {
            sb.append(String.format("【%s】 %s → %s\n",
                    ticket.getTrain().getTrainNumber(), // 适配你model.Train的字段名trainNumber
                    ticket.getDeparture(),
                    ticket.getArrival()));
            sb.append(String.format(" 出发时间：%s | 到达时间：%s | 时长：%d分钟\n",
                    ticket.getDepartureTime(),
                    ticket.getArrivalTime(),
                    ticket.getDuration()));
            sb.append("-------------------------\n");
        }
        return sb.toString();
    }

    // ===================== 接口返回结果接收类（适配你的返回结构） =====================
    public static class TicketQueryResult {
        private List<TicketQueryResp> data;

        // getter/setter（必须有，否则Jackson无法反序列化）
        public List<TicketQueryResp> getData() {
            return data;
        }

        public void setData(List<TicketQueryResp> data) {
            this.data = data;
        }
    }
}