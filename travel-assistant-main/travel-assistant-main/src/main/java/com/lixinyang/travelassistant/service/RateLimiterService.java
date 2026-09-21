package com.lixinyang.travelassistant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口限流器（固定窗口计数）
 * 优先用 Redis 计数：多实例部署时共享同一份计数；
 * Redis 不可用时自动降级为单机内存计数，保证服务不被限流组件拖垮。
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {
    private final StringRedisTemplate redis;

    /** 内存兜底用的窗口 */
    private static class Window {
        long bucket;
        int count;
    }

    private final Map<String, Window> local = new ConcurrentHashMap<>();

    /**
     * @param key           限流维度（如 "llm:127.0.0.1"）
     * @param limit         窗口内允许的最大请求数
     * @param windowSeconds 窗口长度（秒）
     * @return true=放行
     */
    public boolean allow(String key, int limit, long windowSeconds) {
        if (limit <= 0) return true;
        long now = System.currentTimeMillis();
        long bucket = now / (windowSeconds * 1000L);
        try {
            String redisKey = "rl:" + key + ":" + bucket;
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redis.expire(redisKey, Duration.ofSeconds(windowSeconds + 5));
            }
            if (count != null) return count <= limit;
        } catch (Exception ignored) {
            // Redis 异常 → 走内存兜底，不影响业务
        }
        return allowLocal(key, limit, bucket);
    }

    private boolean allowLocal(String key, int limit, long bucket) {
        Window w = local.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            if (w.bucket != bucket) {
                w.bucket = bucket;
                w.count = 0;
            }
            w.count++;
            if (local.size() > 20000) local.clear(); // 防止内存无限增长
            return w.count <= limit;
        }
    }
}
