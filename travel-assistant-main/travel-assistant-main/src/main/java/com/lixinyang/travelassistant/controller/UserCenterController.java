package com.lixinyang.travelassistant.controller;

import com.lixinyang.travelassistant.common.BizException;
import com.lixinyang.travelassistant.entity.Favorite;
import com.lixinyang.travelassistant.entity.Invoice;
import com.lixinyang.travelassistant.entity.Order;
import com.lixinyang.travelassistant.entity.TripPlan;
import com.lixinyang.travelassistant.repository.FavoriteRepository;
import com.lixinyang.travelassistant.repository.InvoiceRepository;
import com.lixinyang.travelassistant.repository.OrderRepository;
import com.lixinyang.travelassistant.repository.TripPlanRepository;
import com.lixinyang.travelassistant.service.NoticeService;
import com.lixinyang.travelassistant.utils.JwtUtil;
import com.lixinyang.travelassistant.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 用户中心：收藏、行程历史、发票申请 */
@RestController
@RequiredArgsConstructor
public class UserCenterController {
    private final FavoriteRepository favoriteRepo;
    private final TripPlanRepository planRepo;
    private final InvoiceRepository invoiceRepo;
    private final OrderRepository orderRepo;
    private final NoticeService noticeService;
    private final JwtUtil jwtUtil;

    // ================== 收藏 ==================

    @GetMapping("/favorite/my")
    public Result<List<Favorite>> myFavorites(@RequestHeader(value = "Authorization", required = false) String auth) {
        return Result.ok(favoriteRepo.findByUsernameOrderByCreateTimeDesc(usernameOf(auth)));
    }

    @GetMapping("/favorite/check")
    public Result<Map<String, Object>> checkFavorite(@RequestParam String type,
                                                     @RequestParam String name,
                                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        boolean fav = favoriteRepo.findByUsernameAndTypeAndName(usernameOf(auth), type, name).isPresent();
        Map<String, Object> m = new HashMap<>();
        m.put("favorited", fav);
        return Result.ok(m);
    }

    /** 点一次收藏，再点一次取消 */
    @PostMapping("/favorite/toggle")
    public Result<Map<String, Object>> toggleFavorite(@RequestBody Favorite req,
                                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        String username = usernameOf(auth);
        if (req.getName() == null || req.getName().isBlank()) throw new BizException("缺少名称");
        String type = req.getType() == null || req.getType().isBlank() ? "HOTEL" : req.getType();

        Map<String, Object> m = new HashMap<>();
        Favorite exist = favoriteRepo.findByUsernameAndTypeAndName(username, type, req.getName()).orElse(null);
        if (exist != null) {
            favoriteRepo.delete(exist);
            m.put("favorited", false);
        } else {
            if (favoriteRepo.countByUsername(username) >= 200) throw new BizException("收藏夹已满（最多 200 个）");
            req.setId(null);
            req.setUsername(username);
            req.setType(type);
            favoriteRepo.save(req);
            m.put("favorited", true);
        }
        return Result.ok(m);
    }

    @DeleteMapping("/favorite/{id}")
    public Result<String> deleteFavorite(@PathVariable Long id,
                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        Favorite f = favoriteRepo.findById(id).orElseThrow(() -> new BizException(404, "收藏不存在"));
        if (!usernameOf(auth).equals(f.getUsername())) throw new BizException(403, "无权删除他人的收藏");
        favoriteRepo.delete(f);
        return Result.ok("已取消收藏");
    }

    // ================== 行程历史 ==================

    @GetMapping("/plan/my")
    public Result<List<TripPlan>> myPlans(@RequestHeader(value = "Authorization", required = false) String auth) {
        return Result.ok(planRepo.findByUsernameOrderByCreateTimeDesc(usernameOf(auth)));
    }

    @GetMapping("/plan/{id}")
    public Result<TripPlan> planDetail(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        TripPlan p = planRepo.findById(id).orElseThrow(() -> new BizException(404, "行程不存在"));
        if (!usernameOf(auth).equals(p.getUsername())) throw new BizException(403, "无权查看他人的行程");
        return Result.ok(p);
    }

    @PostMapping("/plan/save")
    public Result<TripPlan> savePlan(@RequestBody TripPlan req,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        String username = usernameOf(auth);
        if (planRepo.countByUsername(username) >= 50) throw new BizException("最多保存 50 条行程，先删掉一些吧");
        req.setId(null);
        req.setUsername(username);
        TripPlan saved = planRepo.save(req);
        noticeService.push(username, "SYSTEM", "行程已保存",
                "「" + (saved.getCity() == null ? "未命名" : saved.getCity()) + " " + saved.getDays() + " 天」行程已保存，可在「我的行程」里随时回看。");
        return Result.ok(saved);
    }

    @DeleteMapping("/plan/{id}")
    public Result<String> deletePlan(@PathVariable Long id,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        TripPlan p = planRepo.findById(id).orElseThrow(() -> new BizException(404, "行程不存在"));
        if (!usernameOf(auth).equals(p.getUsername())) throw new BizException(403, "无权删除他人的行程");
        planRepo.delete(p);
        return Result.ok("已删除");
    }

    // ================== 发票 ==================

    @GetMapping("/invoice/my")
    public Result<List<Invoice>> myInvoices(@RequestHeader(value = "Authorization", required = false) String auth) {
        return Result.ok(invoiceRepo.findByUsernameOrderByApplyTimeDesc(usernameOf(auth)));
    }

    @PostMapping("/invoice/apply")
    public Result<Invoice> applyInvoice(@RequestBody Invoice req,
                                        @RequestHeader(value = "Authorization", required = false) String auth) {
        String username = usernameOf(auth);
        Order o = orderRepo.findById(req.getOrderId() == null ? -1L : req.getOrderId())
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        if (!username.equals(o.getUsername())) throw new BizException(403, "无权为该订单开票");
        if (!"PAID".equals(o.getPayStatus())) throw new BizException("只有已支付的订单才能开发票");
        if (invoiceRepo.existsByOrderId(o.getId())) throw new BizException("这张订单已经申请过发票了");
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new BizException("请填写发票抬头");

        Invoice inv = new Invoice();
        inv.setUsername(username);
        inv.setOrderId(o.getId());
        inv.setOrderNo(o.getOrderNo());
        inv.setAmount(o.getPrice());
        inv.setTitle(req.getTitle());
        inv.setTaxNo(req.getTaxNo());
        inv.setEmail(req.getEmail());
        inv.setStatus("PENDING");
        Invoice saved = invoiceRepo.save(inv);

        noticeService.push(username, "SYSTEM", "发票申请已提交",
                "订单 " + o.getOrderNo() + " 的发票申请已提交，管理员开票后会通知你。");
        return Result.ok(saved);
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
