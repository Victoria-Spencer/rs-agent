package org.rail.agent.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.rail.agent.dto.AgentTicketQueryDTO;
import org.rail.agent.dto.TicketQueryDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 字段完整性校验工具（适配原有系统DTO）
 */
@Slf4j
@Component
public class FieldCheckUtil {
    // 坐席类型映射（与原有系统一致）
    private static final List<Integer> DEFAULT_SEAT_TYPE = List.of(1); // 1=二等座
    private static final List<Integer> DEFAULT_TRAIN_TYPE = List.of(1, 2); // 1=高铁，2=动车

    /**
     * 校验并转换为原有系统的TicketQueryDTO
     */
    public CheckResult checkAndConvert(AgentTicketQueryDTO agentDTO, StationCodeUtil stationCodeUtil) {
        CheckResult result = new CheckResult();
        TicketQueryDTO ticketDTO = new TicketQueryDTO();
        List<String> missingFields = new ArrayList<>();

        // 1. 校验出发站编码
        List<String> depCodes = stationCodeUtil.getNameToCode(agentDTO.getStartStation());
        if (depCodes.isEmpty()) {
            missingFields.add("出发站（暂不支持你输入的站点，请核对，例如：北京南、西安北）");
        } else {
            ticketDTO.setDepartureCodes(depCodes);
        }

        // 2. 校验到达站编码
        List<String> arrCodes = stationCodeUtil.getNameToCode(agentDTO.getEndStation());
        if (arrCodes.isEmpty()) {
            missingFields.add("到达站（暂不支持你输入的站点，请核对，例如：北京南、西安北）");
        } else {
            ticketDTO.setArrivalCodes(arrCodes);
        }

        // 3. 校验出发日期
        LocalDate departureDate = agentDTO.getTravelTime() != null ? agentDTO.getTravelTime().toLocalDate() : null;
        if (departureDate == null) {
            missingFields.add("出发日期（例如：2026-03-14）");
        } else {
            ticketDTO.setDepartureDate(departureDate);
        }

        // 4. 可选字段：列车类型（默认高铁+动车）
        ticketDTO.setTrainTypeIds(agentDTO.getTrainTypeIds() == null ? DEFAULT_TRAIN_TYPE : agentDTO.getTrainTypeIds());

        // 5. 可选字段：坐席类型（默认二等座）
        ticketDTO.setSeatTypes(DEFAULT_SEAT_TYPE);

        // 构建结果
        if (!missingFields.isEmpty()) {
            result.setMissing(true);
            StringBuilder askMsg = new StringBuilder("为了帮你精准查询车票，请补充以下信息：\n");
            for (String field : missingFields) {
                askMsg.append("- ").append(field).append("\n");
            }
            askMsg.append("示例：2026年3月14日 北京南 到 西安北");
            result.setAskMessage(askMsg.toString());
        } else {
            result.setMissing(false);
            result.setTicketQueryDTO(ticketDTO);
        }

        return result;
    }

    /**
     * 校验结果内部类
     */
    @Data
    public static class CheckResult {
        private boolean missing; // 是否缺失字段
        private String askMessage; // 追问话术
        private TicketQueryDTO ticketQueryDTO; // 转换后的DTO
    }
}