package com.lixinyang.travelassistant.service;

import com.lixinyang.travelassistant.common.BizException;
import com.lixinyang.travelassistant.entity.Order;
import com.lixinyang.travelassistant.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务
 * 所有写操作都带事务；状态流转都做前置校验，避免并发 / 重复操作把数据改坏。
 * 订单状态：已预订 / 已取消
 * 支付状态：UNPAID / PAID
 * 申请状态：PENDING / APPROVED / REJECTED
 * 退款状态：REFUNDED（沙箱退款）
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepo;
    private final NoticeService noticeService;
    private final StockService stockService;

    /** 支付超时时间（分钟）：超过后自动关单 */
    public static final int PAY_EXPIRE_MINUTES = 30;

    /** 下单 */
    @Transactional
    public Order create(Order o) {
        if (o.getPrice() != null && o.getPrice() < 0) throw new BizException("订单金额不合法");
        // 先占库存：不足会抛业务异常，订单不会创建成功
        if (o.getPlaceId() != null && o.getBizDate() != null && o.getRoomType() != null) {
            stockService.lock(o.getPlaceId(), o.getBizDate(), o.getRoomType(), qtyOf(o));
        }
        o.setId(null);
        o.setOrderNo("T" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999));
        o.setStatus("已预订");
        o.setPayStatus("UNPAID");
        o.setChangeStatus("");
        o.setRequestType("");
        o.setRefundStatus("");
        o.setCreateTime(LocalDateTime.now());
        return orderRepo.save(o);
    }

    /** 订单详情（校验归属，防止越权查看别人的订单） */
    public Order get(Long id, String username) {
        Order o = orderRepo.findById(id).orElse(null);
        if (o == null) return null;
        if (username != null && o.getUsername() != null && !o.getUsername().equals(username)) return null;
        return o;
    }

    /**
     * 沙箱（模拟）支付：幂等 —— 已支付订单重复调用直接返回，不会重复扣款。
     * noRollbackFor：超时关单后要抛异常提示用户，但「已取消」这个状态必须落库，所以不能让异常回滚。
     */
    @Transactional(noRollbackFor = BizException.class)
    public Order pay(Long id, String username, String method) {
        Order o = requireOwn(id, username);
        if ("已取消".equals(o.getStatus())) throw new BizException("订单已取消，无法支付");
        if ("PAID".equals(o.getPayStatus())) return o;
        if (isExpired(o)) {
            o.setStatus("已取消");
            o.setChangeStatus("");
            orderRepo.save(o);
            throw new BizException("订单超过 " + PAY_EXPIRE_MINUTES + " 分钟未支付，已自动关闭，请重新下单");
        }
        o.setPayStatus("PAID");
        o.setPayTime(LocalDateTime.now());
        o.setPayMethod(method == null || method.isBlank() ? "ALIPAY" : method);
        o.setPayNo("PAY" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999));
        Order saved = orderRepo.save(o);
        // 只有真正从「未支付」变成「已支付」才发通知，重复支付不会重复发
        noticeService.push(saved.getUsername(), "PAY", "支付成功",
                "订单 " + saved.getOrderNo() + " 已支付 ¥" + saved.getPrice()
                        + "，支付方式 " + saved.getPayMethod() + "，交易流水号 " + saved.getPayNo() + "。");
        return saved;
    }

    /** 定时任务：每 60 秒扫描一次，关闭超过 30 分钟仍未支付的订单（避免虚假占位） */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void closeExpiredUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(PAY_EXPIRE_MINUTES);
        List<Order> expired = orderRepo.findByPayStatusAndCreateTimeBefore("UNPAID", deadline);
        for (Order o : expired) {
            if ("已取消".equals(o.getStatus())) continue;
            if ("PENDING".equals(o.getChangeStatus())) continue;
            o.setStatus("已取消");
            o.setChangeStatus("");
            orderRepo.save(o);
            stockService.release(o.getPlaceId(), o.getBizDate(), o.getRoomType(), qtyOf(o)); // 超时关单回补库存
            System.out.println("[订单超时关单] " + o.getOrderNo() + " 超 " + PAY_EXPIRE_MINUTES + " 分钟未支付，已自动取消");
        }
    }

    /** 我的订单 */
    public List<Order> myOrders(String username) {
        return orderRepo.findByUsernameOrderByCreateTimeDesc(username);
    }

    /** 管理员列表（可按类型过滤） */
    public List<Order> adminList(String type) {
        if (type == null || type.isBlank()) return orderRepo.findAllByOrderByCreateTimeDesc();
        return orderRepo.findByTypeOrderByCreateTimeDesc(type);
    }

    /** 管理员修改（时间 / 出发地 / 目的地 等） */
    @Transactional
    public Order update(Long id, Order patch) {
        Order o = orderRepo.findById(id).orElse(null);
        if (o == null) return null;
        // 改期/改房型要搬库存：先记下旧的
        Long oldPlace = o.getPlaceId();
        String oldDate = o.getBizDate();
        String oldRoom = o.getRoomType();
        if (patch.getFromCity() != null) o.setFromCity(patch.getFromCity());
        if (patch.getToCity() != null) o.setToCity(patch.getToCity());
        if (patch.getTravelDate() != null) o.setTravelDate(patch.getTravelDate());
        if (patch.getDepartTime() != null) o.setDepartTime(patch.getDepartTime());
        if (patch.getArriveTime() != null) o.setArriveTime(patch.getArriveTime());
        if (patch.getSeat() != null) o.setSeat(patch.getSeat());
        if (patch.getPrice() != null) o.setPrice(patch.getPrice());
        if (patch.getStatus() != null) o.setStatus(patch.getStatus());
        if (patch.getPassenger() != null) o.setPassenger(patch.getPassenger());
        if (patch.getPhone() != null) o.setPhone(patch.getPhone());
        if (patch.getPlaceId() != null) o.setPlaceId(patch.getPlaceId());
        if (patch.getBizDate() != null) o.setBizDate(patch.getBizDate());
        if (patch.getRoomType() != null) o.setRoomType(patch.getRoomType());
        if (patch.getQuantity() != null) o.setQuantity(patch.getQuantity());

        boolean moved = !Objects.equals(oldPlace, o.getPlaceId())
                || !Objects.equals(oldDate, o.getBizDate())
                || !Objects.equals(oldRoom, o.getRoomType());
        if (moved && !"已取消".equals(o.getStatus())) {
            // 先占新的（占不到会抛异常并整体回滚），再还旧的，避免改期把库存弄丢
            stockService.lock(o.getPlaceId(), o.getBizDate(), o.getRoomType(), qtyOf(o));
            stockService.release(oldPlace, oldDate, oldRoom, qtyOf(o));
        }
        return orderRepo.save(o);
    }

    /** 管理员删除 */
    @Transactional
    public boolean delete(Long id) {
        Order o = orderRepo.findById(id).orElse(null);
        if (o == null) return false;
        stockService.release(o.getPlaceId(), o.getBizDate(), o.getRoomType(), qtyOf(o)); // 删单回补库存
        orderRepo.deleteById(id);
        return true;
    }

    /** 用户提交改签申请：必须已支付、未取消、且没有正在处理的申请 */
    @Transactional
    public Order applyChange(Long id, Order req) {
        Order o = require(id);
        guardApply(o, "改签");
        if (!"PAID".equals(o.getPayStatus())) throw new BizException("未支付的订单不能申请改签，请先支付或直接取消");
        o.setRequestType("CHANGE");
        o.setChangeStatus("PENDING");
        o.setReqFromCity(req.getFromCity());
        o.setReqToCity(req.getToCity());
        o.setReqTravelDate(req.getTravelDate());
        o.setReqDepartTime(req.getDepartTime());
        o.setReqArriveTime(req.getArriveTime());
        o.setReqSeat(req.getSeat());
        o.setReqPrice(req.getPrice());
        o.setChangeApplyTime(LocalDateTime.now());
        Order saved = orderRepo.save(o);
        noticeService.push(saved.getUsername(), "ORDER", "改签申请已提交",
                "订单 " + saved.getOrderNo() + " 的改签申请已提交，管理员审核后会在这里通知你。");
        return saved;
    }

    /**
     * 用户退订：
     * 未支付 —— 直接取消，不需要审批；
     * 已支付 —— 走申请流程，管理员同意后自动退款（沙箱）。
     */
    @Transactional
    public Order applyCancel(Long id, String username) {
        Order o = requireOwn(id, username);
        if ("已取消".equals(o.getStatus())) throw new BizException("订单已经取消了");
        if ("PENDING".equals(o.getChangeStatus())) throw new BizException("已有待审核的申请，请等管理员处理");
        if (!"PAID".equals(o.getPayStatus())) {
            o.setStatus("已取消");
            o.setChangeStatus("");
            Order saved = orderRepo.save(o);
            stockService.release(saved.getPlaceId(), saved.getBizDate(), saved.getRoomType(), qtyOf(saved)); // 未支付直接取消，回补库存
            noticeService.push(saved.getUsername(), "ORDER", "订单已取消",
                    "订单 " + saved.getOrderNo() + " 未支付，已直接取消，无需等待审核。");
            return saved;
        }
        o.setRequestType("CANCEL");
        o.setChangeStatus("PENDING");
        o.setChangeApplyTime(LocalDateTime.now());
        Order saved = orderRepo.save(o);
        noticeService.push(saved.getUsername(), "ORDER", "退订申请已提交",
                "订单 " + saved.getOrderNo() + " 的退订申请已提交，管理员通过后会自动退款。");
        return saved;
    }

    /** 待审批的申请 */
    public List<Order> pendingChanges() {
        return orderRepo.findByChangeStatusOrderByChangeApplyTimeDesc("PENDING");
    }

    /** 管理员通过：改签写回申请内容；退订置为已取消并退款（沙箱） */
    @Transactional
    public Order approveChange(Long id) {
        Order o = require(id);
        if (!"PENDING".equals(o.getChangeStatus())) throw new BizException("该申请已处理过，请刷新后查看");
        if ("CANCEL".equals(o.getRequestType())) {
            o.setStatus("已取消");
            o.setChangeStatus("APPROVED");
            refundIfPaid(o);
            Order saved = orderRepo.save(o);
            stockService.release(saved.getPlaceId(), saved.getBizDate(), saved.getRoomType(), qtyOf(saved)); // 退订通过，回补库存
            String refundMsg = "REFUNDED".equals(saved.getRefundStatus())
                    ? "退款 ¥" + saved.getPrice() + " 已原路退回（沙箱），退款单号 " + saved.getRefundNo() + "。"
                    : "订单未支付，已直接关闭。";
            noticeService.push(saved.getUsername(), "REFUND", "退订成功", "订单 " + saved.getOrderNo() + " 已取消，" + refundMsg);
            return saved;
        }
        if (o.getReqFromCity() != null) o.setFromCity(o.getReqFromCity());
        if (o.getReqToCity() != null) o.setToCity(o.getReqToCity());
        if (o.getReqTravelDate() != null) o.setTravelDate(o.getReqTravelDate());
        if (o.getReqDepartTime() != null) o.setDepartTime(o.getReqDepartTime());
        if (o.getReqArriveTime() != null) o.setArriveTime(o.getReqArriveTime());
        if (o.getReqSeat() != null) o.setSeat(o.getReqSeat());
        if (o.getReqPrice() != null) o.setPrice(o.getReqPrice());
        o.setChangeStatus("APPROVED");
        Order saved = orderRepo.save(o);
        noticeService.push(saved.getUsername(), "APPROVE", "改签成功",
                "订单 " + saved.getOrderNo() + " 已改签为：" + saved.getFromCity() + " → " + saved.getToCity()
                        + "，" + saved.getTravelDate() + " " + saved.getDepartTime() + "。");
        return saved;
    }

    /** 管理员驳回：只有待审批的申请才能驳回 */
    @Transactional
    public Order rejectChange(Long id) {
        Order o = require(id);
        if (!"PENDING".equals(o.getChangeStatus())) throw new BizException("该申请已处理过，请刷新后查看");
        o.setChangeStatus("REJECTED");
        Order saved = orderRepo.save(o);
        noticeService.push(saved.getUsername(), "APPROVE", "申请被驳回",
                "订单 " + saved.getOrderNo() + " 的申请未通过，如有疑问请联系客服。");
        return saved;
    }

    /** 沙箱退款：已支付的订单把钱退回，写退款流水 */
    private void refundIfPaid(Order o) {
        if (!"PAID".equals(o.getPayStatus())) return;
        o.setRefundStatus("REFUNDED");
        o.setRefundTime(LocalDateTime.now());
        o.setRefundNo("RF" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999));
    }

    private Order require(Long id) {
        Order o = orderRepo.findById(id).orElse(null);
        if (o == null) throw new BizException(404, "订单不存在");
        return o;
    }

    private Order requireOwn(Long id, String username) {
        Order o = require(id);
        if (username != null && o.getUsername() != null && !o.getUsername().equals(username)) {
            throw new BizException(403, "无权操作他人的订单");
        }
        return o;
    }

    private void guardApply(Order o, String action) {
        if ("已取消".equals(o.getStatus())) throw new BizException("订单已取消，不能申请" + action);
        if ("PENDING".equals(o.getChangeStatus())) throw new BizException("已有待审核的申请，请等管理员处理");
    }

    private boolean isExpired(Order o) {
        return o.getCreateTime() != null
                && o.getCreateTime().plusMinutes(PAY_EXPIRE_MINUTES).isBefore(LocalDateTime.now());
    }

    /** 订单占用的库存数量（默认 1 间/张） */
    private int qtyOf(Order o) {
        return (o.getQuantity() == null || o.getQuantity() < 1) ? 1 : o.getQuantity();
    }
}
