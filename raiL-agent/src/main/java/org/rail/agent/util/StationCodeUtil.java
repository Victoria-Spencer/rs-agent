package org.rail.agent.util;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车站名称→编码映射工具（适配原有12306系统编码规则）
 */
@Slf4j
@Component
public class StationCodeUtil {
    // 模拟12306官方车站编码表
    private static final Map<String, String> STATION_CODE_MAP = new HashMap<>();

    static {
        // 核心车站编码（需与原有ticket-service保持一致）
        STATION_CODE_MAP.put("北京南", "VNP");
        STATION_CODE_MAP.put("西安北", "EAY");
        STATION_CODE_MAP.put("上海虹桥", "AOH");
        STATION_CODE_MAP.put("广州南", "IZQ");
        STATION_CODE_MAP.put("深圳北", "IOQ");
    }

    /**
     * 站名转编码（支持模糊匹配）
     */
    public List<String> getNameToCode(String stationName) {
        if (stationName == null || stationName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // 去除冗余字符（如"站"）
        String cleanName = stationName.replace("站", "").trim();
        String code = STATION_CODE_MAP.get(cleanName);

        if (code == null) {
            log.warn("未匹配到车站编码：{}", stationName);
            return Collections.emptyList();
        }
        return CollectionUtil.newArrayList(code);
    }
}