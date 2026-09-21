package com.lixinyang.travelassistant.controller;
import com.lixinyang.travelassistant.common.BizException;
import com.lixinyang.travelassistant.entity.AdminLog;
import com.lixinyang.travelassistant.entity.Invoice;
import com.lixinyang.travelassistant.entity.Stock;
import com.lixinyang.travelassistant.service.AdminService;
import com.lixinyang.travelassistant.entity.Order;
import com.lixinyang.travelassistant.repository.InvoiceRepository;
import com.lixinyang.travelassistant.service.AdminLogService;
import com.lixinyang.travelassistant.service.NoticeService;
import com.lixinyang.travelassistant.service.OrderService;
import com.lixinyang.travelassistant.service.StockService;
import com.lixinyang.travelassistant.utils.JwtUtil;
import com.lixinyang.travelassistant.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@RestController @RequestMapping("/admin") @RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final OrderService orderService;
    private final AdminLogService adminLogService;
    private final InvoiceRepository invoiceRepo;
    private final NoticeService noticeService;
    private final StockService stockService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String token = adminService.login(username, body.getOrDefault("password", ""));
        if (token == null) return Result.fail(401, "管理员账号或密码错误");
        adminLogService.log(username, "LOGIN", username, "管理员登录");
        return Result.ok(token);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminService.stats());
    }

    // ===== 订单管理 =====
    @GetMapping("/orders")
    public Result<List<Order>> orders(@RequestParam(required = false) String type) {
        return Result.ok(orderService.adminList(type));
    }

    @PutMapping("/orders/{id}")
    public Result<Order> updateOrder(@PathVariable Long id, @RequestBody Order patch,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        Order o = orderService.update(id, patch);
        if (o == null) return Result.fail(404, "订单不存在");
        adminLogService.log(adminOf(auth), "UPDATE_ORDER", o.getOrderNo(),
                "修改订单：" + o.getFromCity() + " → " + o.getToCity() + "，" + o.getTravelDate() + " " + o.getDepartTime());
        noticeService.push(o.getUsername(), "SYSTEM", "订单信息被管理员修改",
                "订单 " + o.getOrderNo() + " 的信息已更新为：" + o.getFromCity() + " → " + o.getToCity()
                        + "，" + o.getTravelDate() + " " + o.getDepartTime() + "。");
        return Result.ok(o);
    }

    @DeleteMapping("/orders/{id}")
    public Result<String> deleteOrder(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        boolean ok = orderService.delete(id);
        if (!ok) return Result.fail(404, "订单不存在");
        adminLogService.log(adminOf(auth), "DELETE_ORDER", "ID=" + id, "删除订单");
        return Result.ok("已删除");
    }

    // ===== 改签申请审批 =====
    @GetMapping("/order-changes")
    public Result<List<Order>> orderChanges() {
        return Result.ok(orderService.pendingChanges());
    }

    @PostMapping("/order-changes/{id}/approve")
    public Result<Order> approveChange(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        Order o = orderService.approveChange(id);
        if (o == null) return Result.fail(404, "订单不存在");
        adminLogService.log(adminOf(auth), "APPROVE", o.getOrderNo(),
                ("CANCEL".equals(o.getRequestType()) ? "通过退订" : "通过改签") + "，状态=" + o.getStatus()
                        + (o.getRefundNo() == null ? "" : "，退款单号 " + o.getRefundNo()));
        return Result.ok(o);
    }

    @PostMapping("/order-changes/{id}/reject")
    public Result<Order> rejectChange(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        Order o = orderService.rejectChange(id);
        if (o == null) return Result.fail(404, "订单不存在");
        adminLogService.log(adminOf(auth), "REJECT", o.getOrderNo(), "驳回用户的改签/退订申请");
        return Result.ok(o);
    }

    // ===== 发票管理 =====
    @GetMapping("/invoices")
    public Result<List<Invoice>> invoices(@RequestParam(required = false) String status) {
        return Result.ok((status == null || status.isBlank())
                ? invoiceRepo.findAllByOrderByApplyTimeDesc()
                : invoiceRepo.findByStatusOrderByApplyTimeDesc(status));
    }

    @PostMapping("/invoices/{id}/issue")
    public Result<Invoice> issueInvoice(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String auth) {
        Invoice inv = invoiceRepo.findById(id).orElseThrow(() -> new BizException(404, "发票申请不存在"));
        if (!"PENDING".equals(inv.getStatus())) throw new BizException("这张发票已经处理过了");
        inv.setStatus("ISSUED");
        inv.setIssueTime(LocalDateTime.now());
        Invoice saved = invoiceRepo.save(inv);
        adminLogService.log(adminOf(auth), "ISSUE_INVOICE", saved.getOrderNo(), "开票：" + saved.getTitle());
        noticeService.push(saved.getUsername(), "SYSTEM", "发票已开具",
                "订单 " + saved.getOrderNo() + " 的发票（¥" + saved.getAmount() + "）已开具，抬头：" + saved.getTitle() + "。");
        return Result.ok(saved);
    }

    @PostMapping("/invoices/{id}/reject")
    public Result<Invoice> rejectInvoice(@PathVariable Long id,
                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        Invoice inv = invoiceRepo.findById(id).orElseThrow(() -> new BizException(404, "发票申请不存在"));
        if (!"PENDING".equals(inv.getStatus())) throw new BizException("这张发票已经处理过了");
        inv.setStatus("REJECTED");
        Invoice saved = invoiceRepo.save(inv);
        adminLogService.log(adminOf(auth), "REJECT_INVOICE", saved.getOrderNo(), "驳回用户的发票申请");
        noticeService.push(saved.getUsername(), "SYSTEM", "发票申请被驳回",
                "订单 " + saved.getOrderNo() + " 的发票申请未通过，请联系客服了解原因。");
        return Result.ok(saved);
    }

    // ===== 操作审计日志 =====
    @GetMapping("/logs")
    public Result<List<AdminLog>> logs() {
        return Result.ok(adminLogService.recent());
    }

    // ===== 库存管理 =====
    @GetMapping("/stocks")
    public Result<List<Stock>> stocks(@RequestParam Long placeId) {
        return Result.ok(stockService.listByPlace(placeId));
    }

    /** 维护库存：同一个「地点 + 日期 + 房型」有就改，没有就新增 */
    @PostMapping("/stocks")
    public Result<Stock> saveStock(@RequestBody Map<String, Object> body,
                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        Long placeId = body.get("placeId") == null ? null : Long.valueOf(String.valueOf(body.get("placeId")));
        String bizDate = body.get("bizDate") == null ? null : String.valueOf(body.get("bizDate"));
        String roomType = body.get("roomType") == null ? null : String.valueOf(body.get("roomType"));
        Integer total = body.get("total") == null ? null : Integer.valueOf(String.valueOf(body.get("total")));
        Double price = body.get("price") == null || String.valueOf(body.get("price")).isBlank()
                ? null : Double.valueOf(String.valueOf(body.get("price")));

        Stock saved = stockService.save(placeId, bizDate, roomType, total, price);
        adminLogService.log(adminOf(auth), "SAVE_STOCK", "placeId=" + placeId,
                bizDate + " " + roomType + " 库存设为 " + total);
        return Result.ok(saved);
    }

    private String adminOf(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return "unknown";
        try {
            return jwtUtil.parseUsername(auth.substring(7));
        } catch (Exception e) {
            return "unknown";
        }
    }
}
