package com.lixinyang.travelassistant.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixinyang.travelassistant.service.UserService;
import com.lixinyang.travelassistant.utils.JwtUtil;
import com.lixinyang.travelassistant.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public AuthInterceptor(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String auth = request.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
        if (token == null || !userService.isValid(token)) {
            return deny(response, 401, "未登录或登录已过期");
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin")) {
            String role = jwtUtil.parseRole(token);
            if (!"ADMIN".equals(role)) {
                return deny(response, 403, "无管理员权限");
            }
        }
        return true;
    }

    private boolean deny(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(Result.fail(status, msg)));
        return false;
    }
}