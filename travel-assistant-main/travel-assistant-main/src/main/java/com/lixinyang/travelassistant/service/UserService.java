package com.lixinyang.travelassistant.service;

import com.lixinyang.travelassistant.entity.User;
import com.lixinyang.travelassistant.repository.UserRepository;
import com.lixinyang.travelassistant.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public boolean register(String username, String password, String email) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        if (userRepository.existsByUsername(username)) {
            return false;
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setEmail(email);
        userRepository.save(u);
        return true;
    }

    public String login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(u -> encoder.matches(password, u.getPassword()))
                .map(u -> jwtUtil.generate(u.getId(), u.getUsername()))
                .orElse(null);
    }

    public boolean isValid(String token) {
        if (token == null) return false;
        if (isBlacklisted(token)) return false;   // 已登出的 token 立即失效
        return jwtUtil.validate(token);
    }

    public String getUsername(String token) {
        return jwtUtil.parseUsername(token);
    }

    /** 修改密码：校验原密码，新密码至少 6 位 */
    public boolean changePassword(String username, String oldPwd, String newPwd) {
        if (username == null || oldPwd == null || newPwd == null || newPwd.length() < 6) return false;
        return userRepository.findByUsername(username)
                .filter(u -> encoder.matches(oldPwd, u.getPassword()))
                .map(u -> {
                    u.setPassword(encoder.encode(newPwd));
                    userRepository.save(u);
                    return true;
                })
                .orElse(false);
    }

    public void logout(String token) {
        // JWT 本身无状态，登出要把 token 拉黑到「它自然过期」为止，否则退出登录等于没退
        long left = jwtUtil.remainingMillis(token);
        if (left <= 0) return;
        try {
            redisTemplate.opsForValue().set(blackKey(token), "1", Duration.ofMillis(left));
        } catch (Exception ignored) {
            // Redis 挂了不阻断登出：前端已清 token，服务端等它到期自然失效
        }
    }

    private String blackKey(String token) {
        return "jwt:black:" + token;
    }

    private boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(blackKey(token)));
        } catch (Exception e) {
            return false; // Redis 异常时不误杀正常请求
        }
    }
}
