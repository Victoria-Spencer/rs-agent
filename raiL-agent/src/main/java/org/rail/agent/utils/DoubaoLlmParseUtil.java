package org.rail.agent.utils;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import okhttp3.*;
import org.rail.agent.enums.LlmParseIntentEnum;
import org.rail.agent.pojo.dto.AgentTicketBuyDTO;
import org.rail.agent.pojo.dto.AgentTicketQueryDTO;
import org.rail.agent.result.LlmParseResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 豆包LLM解析工具类（优化Reply生成逻辑，贴合前端展示需求）
 * 核心：根据不同意图+DTO数据动态生成业务化Reply，支持查询/购票场景
 */
@Component
public class DoubaoLlmParseUtil {
    // ========== 配置项 ==========
    @Value("${doubao.llm.api-key}")
    private String apiKey;
    @Value("${doubao.llm.base-url}")
    private String baseUrl;
    @Value("${doubao.llm.model}")
    private String model;
    @Value("${doubao.llm.timeout:30}")
    private int timeout;

    // 席别类型映射
    private static final Map<String, Integer> SEAT_TYPE_MAPPING = new HashMap<>();

    static {
        SEAT_TYPE_MAPPING.put("二等座", 1);
        SEAT_TYPE_MAPPING.put("一等座", 2);
        SEAT_TYPE_MAPPING.put("商务座", 3);
        SEAT_TYPE_MAPPING.put("硬座", 4);
        SEAT_TYPE_MAPPING.put("硬卧", 5);
    }

    // OkHttp客户端
    private OkHttpClient client;

    @PostConstruct
    public void initClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    // ========== 核心方法：自动识别意图+解析+生成业务化Reply ==========
    public LlmParseResult autoParse(String userInput) {
        LlmParseResult result = new LlmParseResult();
        try {
            // 1. 识别意图+原始参数
            JSONObject intentAndParams = recognizeIntentAndParams(userInput);
            String intentStr = intentAndParams.getStr("intent");
            if (intentStr == null || intentStr.isEmpty()) {
                result.setReply("抱歉，未识别到有效的业务意图，请重新描述~");
                return result;
            }

            // 2. 处理无匹配意图
            if ("OTHER".equals(intentStr)) {
                result.setReply(String.format("抱歉，暂不支持该业务场景（仅支持：%s），请重新描述~",
                        String.join("、", LlmParseIntentEnum.getAllIntentNames())));
                result.setIntent(null);
                result.setData(null);
                return result;
            }

            // 3. 匹配枚举意图
            LlmParseIntentEnum intentEnum = LlmParseIntentEnum.getByIntentStr(intentStr);
            if (intentEnum == null) {
                result.setReply(String.format("识别到异常意图：%s（仅支持：%s），请重新描述~",
                        intentStr, String.join("、", LlmParseIntentEnum.getAllIntentNames())));
                result.setIntent(null);
                result.setData(null);
                return result;
            }
            result.setIntent(intentStr);

            // 4. 解析规范参数
            JSONObject rawParams = intentAndParams.getJSONObject("params");
            JSONObject standardParams = parseParamsWithTemplate(rawParams.toString(), intentEnum);

            // 5. 解析为DTO
            Object dto = parseParamsToDTO(standardParams, intentEnum);
            result.setData(dto);

            // 6. 生成业务化Reply（核心优化点）
            result.setReply(generateBusinessReply(intentEnum, dto));

        } catch (IllegalArgumentException e) {
            result.setReply(String.format("参数解析失败：%s，请检查输入格式~", e.getMessage()));
            result.setIntent(null);
            result.setData(null);
            e.printStackTrace();
        } catch (IOException e) {
            result.setReply("服务暂不可用，请稍后再试~");
            result.setIntent(null);
            result.setData(null);
            e.printStackTrace();
        } catch (Exception e) {
            result.setReply(String.format("处理失败：%s，请重新尝试~", e.getMessage()));
            result.setIntent(null);
            result.setData(null);
            e.printStackTrace();
        }
        return result;
    }

    // ========== 核心优化：生成业务化Reply ==========
    /**
     * 根据意图+DTO生成贴合前端展示的Reply
     * @param intentEnum 识别的意图枚举
     * @param dto 解析后的业务DTO
     * @return 业务化回复话术
     */
    private String generateBusinessReply(LlmParseIntentEnum intentEnum, Object dto) {
        switch (intentEnum) {
            case QUERY_TICKET:
                return generateQueryReply((AgentTicketQueryDTO) dto);
            case BUY_TICKET:
                return generateBuyReply((AgentTicketBuyDTO) dto);
            default:
                return String.format("已识别%s意图，参数解析完成~", intentEnum.getIntentDesc());
        }
    }

    /**
     * 生成查询意图的Reply（贴合前端展示：引导查看车票信息）
     */
    private String generateQueryReply(AgentTicketQueryDTO queryDTO) {
        // 字段兜底（避免空值）
        String startStation = Optional.ofNullable(queryDTO.getStartStation()).orElse("出发站");
        String endStation = Optional.ofNullable(queryDTO.getEndStation()).orElse("到达站");
        LocalDate travelDate = queryDTO.getTravelDate();
        String dateStr = travelDate != null ?
                String.format("%d年%d月%d日", travelDate.getYear(), travelDate.getMonthValue(), travelDate.getDayOfMonth()) :
                "指定时间";
        String seatType = Optional.ofNullable(queryDTO.getSeatType()).orElse("席别");

        // 业务化话术（前端可直接展示）
        return String.format("以下是为你查询到%s%s→%s的%s车票信息：",
                dateStr, startStation, endStation, seatType);
    }

    /**
     * 生成购票意图的Reply（贴合前端展示：引导支付操作）
     */
    private String generateBuyReply(AgentTicketBuyDTO buyDTO) {
        // 字段兜底
        String startStation = Optional.ofNullable(buyDTO.getStartStation()).orElse("出发站");
        String endStation = Optional.ofNullable(buyDTO.getEndStation()).orElse("到达站");
        LocalDate travelDate = buyDTO.getTravelDate();
        String dateStr = travelDate != null ?
                String.format("%d年%d月%d日", travelDate.getYear(), travelDate.getMonthValue(), travelDate.getDayOfMonth()) :
                "指定时间";
        String trainNo = Optional.ofNullable(buyDTO.getTrainNo()).orElse("");
        String seatType = Optional.ofNullable(buyDTO.getSeatType()).orElse("席别");
        Integer ticketNum = Optional.ofNullable(buyDTO.getTicketNum()).orElse(1);

        // 业务化话术（包含支付引导，前端可拼接支付按钮）
        if (trainNo.isEmpty()) {
            return String.format("已为你预订%s%s→%s的%s%d张，请选择具体车次后点击下方按钮完成支付：",
                    dateStr, startStation, endStation, seatType, ticketNum);
        } else {
            return String.format("已为你预订%s%s→%s%s次列车%s%d张，请点击下方按钮完成支付：",
                    dateStr, startStation, endStation, trainNo, seatType, ticketNum);
        }
    }

    // ========== 原有内部方法（仅优化异常提示） ==========
    private JSONObject recognizeIntentAndParams(String userInput) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.1);
        requestBody.put("stream", false);

        JSONArray messages = JSONUtil.createArray();
        JSONObject systemMsg = JSONUtil.createObj();
        systemMsg.put("role", "system");
        systemMsg.put("content", LlmParseIntentEnum.getIntentRecognizeTemplate());
        messages.add(systemMsg);

        JSONObject userMsg = JSONUtil.createObj();
        userMsg.put("role", "user");
        userMsg.put("content", userInput);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        Request request = new Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .post(RequestBody.create(
                        MediaType.parse("application/json;charset=UTF-8"),
                        requestBody.toString()
                ))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("意图识别接口调用失败（状态码：" + response.code() + "）");
            }
            String rawResponse = response.body().string();
            String content = extractLlmContent(rawResponse);
            return JSONUtil.parseObj(content);
        }
    }

    private JSONObject parseParamsWithTemplate(String rawParamsJson, LlmParseIntentEnum intentEnum) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.1);
        requestBody.put("stream", false);

        JSONArray messages = JSONUtil.createArray();
        JSONObject systemMsg = JSONUtil.createObj();
        systemMsg.put("role", "system");
        systemMsg.put("content", intentEnum.getParamParseTemplate());
        messages.add(systemMsg);

        JSONObject userMsg = JSONUtil.createObj();
        userMsg.put("role", "user");
        userMsg.put("content", "请严格按照格式解析以下参数：" + rawParamsJson);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        Request request = new Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .post(RequestBody.create(
                        MediaType.parse("application/json;charset=UTF-8"),
                        requestBody.toString()
                ))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("参数格式化接口调用失败（状态码：" + response.code() + "）");
            }
            String rawResponse = response.body().string();
            String content = extractLlmContent(rawResponse);
            return JSONUtil.parseObj(content);
        }
    }

    private Object parseParamsToDTO(JSONObject params, LlmParseIntentEnum intentEnum) {
        Class<?> targetClass = intentEnum.getTargetDtoClass();
        Object dto = ReflectUtil.newInstance(targetClass);
        setCommonFields(params, dto, intentEnum);

        if (targetClass == AgentTicketQueryDTO.class) {
            return dto;
        } else if (targetClass == AgentTicketBuyDTO.class) {
            AgentTicketBuyDTO buyDTO = (AgentTicketBuyDTO) dto;
            buyDTO.setUserId(params.getStr("userId"));
            buyDTO.setTicketNum(params.getInt("ticketNum"));
            buyDTO.setTrainNo(params.getStr("trainNo"));

            String seatType = params.getStr("seatType");
            if (seatType != null && !seatType.isEmpty()) {
                Integer seatTypeId = SEAT_TYPE_MAPPING.get(seatType);
                buyDTO.setSeatTypes(seatTypeId != null ? Collections.singletonList(seatTypeId) : new ArrayList<>());
            } else {
                buyDTO.setSeatTypes(new ArrayList<>());
            }
            return buyDTO;
        } else {
            throw new IllegalArgumentException("暂不支持解析为" + targetClass.getSimpleName() + "类型");
        }
    }

    private void setCommonFields(JSONObject params, Object dto, LlmParseIntentEnum intentEnum) {
        // 出发站/到达站
        ReflectUtil.setFieldValue(dto, "startStation", params.getStr("startStation"));
        ReflectUtil.setFieldValue(dto, "endStation", params.getStr("endStation"));
        // 席别
        ReflectUtil.setFieldValue(dto, "seatType", params.getStr("seatType"));
        // 出行时间
        String travelTime = params.getStr("travelTime");
        if (travelTime == null || travelTime.isEmpty()) {
            throw new IllegalArgumentException("出行时间不能为空，请补充出行时间（如：2026-03 或 2026-03-14 14:00）");
        }

        try {
            if (intentEnum == LlmParseIntentEnum.QUERY_TICKET) {
                YearMonth yearMonth = YearMonth.parse(travelTime, DatePattern.NORM_YEAR_MONTH_FORMATTER);
                LocalDate travelDate = yearMonth.atDay(1);
                ReflectUtil.setFieldValue(dto, "travelDate", travelDate);
            } else if (intentEnum == LlmParseIntentEnum.BUY_TICKET) {
                LocalDate travelDate = LocalDate.parse(travelTime.substring(0, 10), DatePattern.NORM_DATE_FORMATTER);
                ReflectUtil.setFieldValue(dto, "travelDate", travelDate);
            }
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            String formatTip = intentEnum == LlmParseIntentEnum.QUERY_TICKET ? "yyyy-MM（如2026-03）" : "yyyy-MM-dd HH:mm（如2026-03-14 14:00）";
            throw new IllegalArgumentException("出行时间格式错误，需为" + formatTip + "，当前输入：" + travelTime);
        }
    }

    private String extractLlmContent(String llmRawResponse) {
        if (!JSONUtil.isJson(llmRawResponse)) {
            throw new IllegalArgumentException("LLM返回非JSON格式响应");
        }

        JSONObject responseJson = JSONUtil.parseObj(llmRawResponse);
        JSONArray choices = responseJson.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("LLM响应无有效结果");
        }

        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        if (message == null) {
            throw new IllegalArgumentException("LLM响应无消息内容");
        }

        String content = message.getStr("content");
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("LLM响应内容为空");
        }
        return content;
    }

    // ========== Setter（测试用） ==========
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}