package com.lixinyang.travelassistant.controller;

import com.lixinyang.travelassistant.service.UserService;
import com.lixinyang.travelassistant.service.UsageLogService;
import com.lixinyang.travelassistant.utils.JwtUtil;
import com.lixinyang.travelassistant.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final UsageLogService usageLogService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<String> register(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        String email = body.getOrDefault("email", "");
        if (username.isBlank() || password.isBlank()) return Result.fail(400, "用户名或密码不能为空");
        boolean ok = userService.register(username, password, email);
        if (!ok) return Result.fail(400, "注册失败，用户名可能已存在");
        return Result.ok("注册成功");
    }

    @PostMapping("/login")
    public ResponseEntity<Result<String>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String token = userService.login(username, body.getOrDefault("password", ""));
        if (token == null) return ResponseEntity.status(401).body(Result.fail(401, "用户名或密码错误"));
        usageLogService.record(username, "LOGIN");
        return ResponseEntity.ok(Result.ok(token));
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) userService.logout(auth.substring(7));
        return Result.ok(null);
    }

    /** 修改密码：校验原密码；改完当前 token 立即失效，需要重新登录 */
    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody Map<String, String> body,
                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return Result.fail(401, "请先登录");
        String token = auth.substring(7);
        String username;
        try {
            username = jwtUtil.parseUsername(token);
        } catch (Exception e) {
            return Result.fail(401, "登录已过期，请重新登录");
        }
        String oldPwd = body.getOrDefault("oldPassword", "");
        String newPwd = body.getOrDefault("newPassword", "");
        if (newPwd.length() < 6) return Result.fail(400, "新密码至少 6 位");
        if (oldPwd.equals(newPwd)) return Result.fail(400, "新密码不能和原密码相同");
        if (!userService.changePassword(username, oldPwd, newPwd)) return Result.fail(400, "原密码不正确");
        userService.logout(token); // 改完密码，当前登录态失效
        return Result.ok("密码修改成功，请重新登录");
    }

}
