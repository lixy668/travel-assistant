package com.lixinyang.travelassistant.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixinyang.travelassistant.service.RateLimiterService;
import com.lixinyang.travelassistant.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口限流拦截器：按 IP + 接口分组限流，超过阈值返回 429。
 * 分三档：AI 类接口（最贵，限得最狠）、订单类、登录注册类，其余走默认档。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RateLimiterService limiter;

    @Value("${ratelimit.enabled:true}")
    private boolean enabled;
    @Value("${ratelimit.per-minute:120}")
    private int perMinute;
    @Value("${ratelimit.llm-per-minute:20}")
    private int llmPerMinute;
    @Value("${ratelimit.order-per-minute:30}")
    private int orderPerMinute;
    @Value("${ratelimit.login-per-minute:10}")
    private int loginPerMinute;

    public RateLimitInterceptor(RateLimiterService limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled) return true;

        String uri = request.getRequestURI();
        String bucket = "api";
        int limit = perMinute;

        if (uri.startsWith("/travel/chat") || uri.startsWith("/travel/recommend") || uri.startsWith("/travel/intro")) {
            bucket = "llm";
            limit = llmPerMinute;
        } else if (uri.startsWith("/order")) {
            bucket = "order";
            limit = orderPerMinute;
        } else if (uri.startsWith("/auth/login") || uri.startsWith("/auth/register") || uri.startsWith("/admin/login")) {
            bucket = "login";
            limit = loginPerMinute;
        }

        if (limiter.allow(bucket + ":" + clientIp(request), limit, 60)) return true;

        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(Result.fail(429, "请求过于频繁，请稍后再试")));
        return false;
    }

    /** 取真实客户端 IP（经过 Nginx 代理后要读 X-Forwarded-For） */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int idx = xff.indexOf(',');
            return (idx > 0 ? xff.substring(0, idx) : xff).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
