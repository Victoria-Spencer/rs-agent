package org.rail.agent.util;

import cn.hutool.core.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.rail.agent.dto.AgentTicketQueryDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 对话上下文管理（Redis存储，适配Spring Boot 3.x）
 */
@Slf4j
@Component
public class DialogContextUtil {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 上下文过期时间：30分钟
    private static final long EXPIRE_MINUTES = 30;
    // 缓存Key前缀
    private static final String CONTEXT_KEY_PREFIX = "rail:agent:dialog:context:";

    /**
     * 保存上下文
     */
    public void saveContext(Long userId, AgentTicketQueryDTO agentDTO) {
        try {
            String key = CONTEXT_KEY_PREFIX + userId;
            String json = JSONUtil.toJsonStr(agentDTO);
            stringRedisTemplate.opsForValue().set(key, json, EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.info("保存用户{}对话上下文成功", userId);
        } catch (Exception e) {
            log.error("保存上下文失败：", e);
        }
    }

    /**
     * 获取上下文
     */
    public AgentTicketQueryDTO getContext(Long userId) {
        try {
            String key = CONTEXT_KEY_PREFIX + userId;
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return JSONUtil.toBean(json, AgentTicketQueryDTO.class);
        } catch (Exception e) {
            log.error("获取上下文失败：", e);
            return null;
        }
    }

    /**
     * 清除上下文
     */
    public void clearContext(Long userId) {
        try {
            String key = CONTEXT_KEY_PREFIX + userId;
            stringRedisTemplate.delete(key);
            log.info("清除用户{}对话上下文成功", userId);
        } catch (Exception e) {
            log.error("清除上下文失败：", e);
        }
    }
}