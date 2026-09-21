package com.lixinyang.travelassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lixinyang.travelassistant.entity.Order;
import com.lixinyang.travelassistant.service.IdempotencyService;
import com.lixinyang.travelassistant.service.OrderService;
import com.lixinyang.travelassistant.service.NoticeService;
import com.lixinyang.travelassistant.service.UsageLogService;
import com.lixinyang.travelassistant.utils.JwtUtil;
import com.lixinyang.travelassistant.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final NoticeService noticeService;
    private final UsageLogService usageLogService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /** 下单（用户）：带 Idempotency-Key 时，10 分钟内重复提交只创建一单（防连点 / 网络重试重复下单） */
    @PostMapping("/create")
    public Result<Order> create(@RequestBody Order req,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
                                @RequestHeader(value = "Authorization", required = false) String auth) {
        String username = usernameOf(auth);
        String key = (idemKey == null || idemKey.isBlank())
                ? null
                : "idem:order:" + username + ":" + idemKey.trim();

        if (key != null) {
            String cached = idempotencyService.get(key);
            if (cached != null) {
                Order old = parseOrder(cached);
                if (old != null) return Result.ok(old); // 直接返回上次那张订单，不重复创建
            }
        }

        req.setUsername(username);
        Order saved = orderService.create(req);

        if (key != null) {
            try {
                idempotencyService.put(key, objectMapper.writeValueAsString(saved));
            } catch (Exception ignored) {
            }
        }
        usageLogService.record(username, "ORDER");
        noticeService.push(username, "ORDER", "订单提交成功",
                "订单 " + saved.getOrderNo() + " 已创建，应付 ¥" + saved.getPrice()
                        + "，请在 " + OrderService.PAY_EXPIRE_MINUTES + " 分钟内完成支付，超时订单会自动关闭。");
        return Result.ok(saved);
    }

    private Order parseOrder(String json) {
        try {
            return objectMapper.readValue(json, Order.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** 我的订单（用户） */
    @GetMapping("/my")
    public Result<List<Order>> my(@RequestHeader(value = "Authorization", required = false) String auth) {
        return Result.ok(orderService.myOrders(usernameOf(auth)));
    }

    /** 订单详情（用户）：收银台页面按订单号重新拉取真实数据 */
    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id,
                                @RequestHeader(value = "Authorization", required = false) String auth) {
        Order o = orderService.get(id, usernameOf(auth));
        if (o == null) return Result.fail(404, "订单不存在或无权限");
        return Result.ok(o);
    }

    /** 用户申请改签 */
    @PostMapping("/change/{id}")
    public Result<Order> applyChange(@PathVariable Long id, @RequestBody Order req,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        Order o = orderService.applyChange(id, req);
        if (o == null) return Result.fail(404, "订单不存在");
        usageLogService.record(usernameOf(auth), "ORDER_CHANGE");
        return Result.ok(o);
    }

    /** 用户申请退订（提交后由管理员审批） */
    @PostMapping("/cancel/{id}")
    public Result<Order> cancel(@PathVariable Long id,
                                @RequestHeader(value = "Authorization", required = false) String auth) {
        Order o = orderService.applyCancel(id, usernameOf(auth));
        if (o == null) return Result.fail(404, "订单不存在或无权限");
        usageLogService.record(usernameOf(auth), "ORDER_CANCEL_APPLY");
        return Result.ok(o);
    }

    /** 沙箱支付（模拟）：点击即支付成功；带支付方式，且幂等（重复点击不会重复扣款） */
    @PostMapping("/pay/{id}")
    public Result<Order> pay(@PathVariable Long id,
                             @RequestBody(required = false) Map<String, String> body,
                             @RequestHeader(value = "Authorization", required = false) String auth) {
        String method = body == null ? null : body.get("method");
        Order o = orderService.pay(id, usernameOf(auth), method);
        if (o == null) return Result.fail(404, "订单不存在或无权限");
        usageLogService.record(usernameOf(auth), "ORDER_PAY");
        return Result.ok(o);
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
