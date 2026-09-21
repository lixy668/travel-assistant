package com.lixinyang.travelassistant.controller;

import com.lixinyang.travelassistant.entity.Notice;
import com.lixinyang.travelassistant.service.NoticeService;
import com.lixinyang.travelassistant.utils.JwtUtil;
import com.lixinyang.travelassistant.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;
    private final JwtUtil jwtUtil;

    /** 消息列表 */
    @GetMapping("/my")
    public Result<List<Notice>> my(@RequestHeader(value = "Authorization", required = false) String auth) {
        return Result.ok(noticeService.my(usernameOf(auth)));
    }

    /** 未读数（前端铃铛红点） */
    @GetMapping("/unread")
    public Result<Map<String, Object>> unread(@RequestHeader(value = "Authorization", required = false) String auth) {
        Map<String, Object> m = new HashMap<>();
        m.put("unread", noticeService.unread(usernameOf(auth)));
        return Result.ok(m);
    }

    @PostMapping("/read/{id}")
    public Result<String> read(@PathVariable Long id,
                               @RequestHeader(value = "Authorization", required = false) String auth) {
        noticeService.read(id, usernameOf(auth));
        return Result.ok("已读");
    }

    @PostMapping("/read-all")
    public Result<String> readAll(@RequestHeader(value = "Authorization", required = false) String auth) {
        noticeService.readAll(usernameOf(auth));
        return Result.ok("全部已读");
    }

    private String usernameOf(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return "anonymous";
        try {
            return jwtUtil.parseUsername(auth.substring(7));
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
