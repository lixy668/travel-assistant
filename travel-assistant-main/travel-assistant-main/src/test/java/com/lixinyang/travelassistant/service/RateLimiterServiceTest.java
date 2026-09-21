package com.lixinyang.travelassistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 限流器单元测试：Redis 正常计数 + Redis 故障时降级到内存 */
@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> ops;

    private RateLimiterService limiter;

    @BeforeEach
    void setUp() {
        limiter = new RateLimiterService(redis);
    }

    @Test
    @DisplayName("Redis 正常时：超过阈值返回 false，并且第一次请求会设置过期时间")
    void limitWorksWithRedis() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L, 2L, 3L);

        assertTrue(limiter.allow("llm:127.0.0.1", 2, 60));
        assertTrue(limiter.allow("llm:127.0.0.1", 2, 60));
        assertFalse(limiter.allow("llm:127.0.0.1", 2, 60), "第 3 次应该被限流");
        verify(redis, times(1)).expire(anyString(), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("Redis 挂掉时：自动降级为内存计数，服务照常可用")
    void fallbackToLocalWhenRedisDown() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        assertTrue(limiter.allow("api:1.1.1.1", 2, 60));
        assertTrue(limiter.allow("api:1.1.1.1", 2, 60));
        assertFalse(limiter.allow("api:1.1.1.1", 2, 60), "降级后依然要限流");
    }

    @Test
    @DisplayName("不同 IP 之间互不影响")
    void differentKeysAreIndependent() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(5L, 1L);

        assertFalse(limiter.allow("api:1.1.1.1", 2, 60));
        assertTrue(limiter.allow("api:2.2.2.2", 2, 60));
    }
}
