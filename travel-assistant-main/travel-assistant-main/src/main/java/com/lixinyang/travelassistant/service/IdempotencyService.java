package com.lixinyang.travelassistant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等服务：同一个幂等键在 TTL 内只允许成功执行一次。
 * 用途：防止用户连点 / 网络重试 / 前端超时重发造成重复下单。
 * Redis 异常时降级为本地内存，单机依然有效。
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final Map<String, Entry> local = new ConcurrentHashMap<>();

    private static class Entry {
        String value;
        long expireAt;

        Entry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    /** 取上次执行结果，没有则返回 null */
    public String get(String key) {
        try {
            String v = redis.opsForValue().get(key);
            if (v != null && !v.isBlank()) return v;
        } catch (Exception ignored) {
        }
        Entry e = local.get(key);
        if (e == null) return null;
        if (e.expireAt < System.currentTimeMillis()) {
            local.remove(key);
            return null;
        }
        return e.value;
    }

    /** 记录本次执行结果（TTL 10 分钟） */
    public void put(String key, String value) {
        try {
            redis.opsForValue().set(key, value, TTL);
        } catch (Exception ignored) {
        }
        if (local.size() > 20000) local.clear();
        local.put(key, new Entry(value, System.currentTimeMillis() + TTL.toMillis()));
    }
}
