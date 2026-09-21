package com.lixinyang.travelassistant.config;

import com.lixinyang.travelassistant.common.AuthInterceptor;
import com.lixinyang.travelassistant.common.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    /** 允许跨域的来源，逗号分隔；开发默认 *，上线用环境变量 CORS_ALLOWED_ORIGINS 收紧 */
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    public WebConfig(AuthInterceptor authInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 顺序：先限流（防刷），再鉴权
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**").order(0);
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/travel/**", "/admin/**", "/order/**",
                        "/favorite/**", "/plan/**", "/invoice/**", "/notice/**")
                .excludePathPatterns("/admin/login")
                .order(1);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
