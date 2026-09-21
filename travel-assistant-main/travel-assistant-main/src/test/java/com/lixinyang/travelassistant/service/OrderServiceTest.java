package com.lixinyang.travelassistant.service;

import com.lixinyang.travelassistant.common.BizException;
import com.lixinyang.travelassistant.entity.Order;
import com.lixinyang.travelassistant.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 订单状态机单元测试：
 * 支付幂等、超时关单、退订审批、重复审批拦截、沙箱退款。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepo;
    @InjectMocks
    private OrderService orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setOrderNo("T1001");
        order.setUsername("lixinyang");
        order.setType("HOTEL");
        order.setStatus("已预订");
        order.setPayStatus("UNPAID");
        order.setChangeStatus("");
        order.setPrice(328.0);
        order.setCreateTime(LocalDateTime.now());
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ================= 支付 =================

    @Test
    @DisplayName("正常支付：写入支付方式、支付时间、流水号")
    void paySuccess() {
        Order r = orderService.pay(1L, "lixinyang", "WECHAT");
        assertEquals("PAID", r.getPayStatus());
        assertEquals("WECHAT", r.getPayMethod());
        assertNotNull(r.getPayTime());
        assertTrue(r.getPayNo().startsWith("PAY"));
    }

    @Test
    @DisplayName("重复支付：幂等，不会重复写库扣款")
    void payIsIdempotent() {
        order.setPayStatus("PAID");
        order.setPayNo("PAY-OLD");
        Order r = orderService.pay(1L, "lixinyang", "ALIPAY");
        assertEquals("PAY-OLD", r.getPayNo(), "重复支付不应该换流水号");
        verify(orderRepo, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("订单已取消：不能支付")
    void payRejectedWhenCancelled() {
        order.setStatus("已取消");
        BizException e = assertThrows(BizException.class, () -> orderService.pay(1L, "lixinyang", "ALIPAY"));
        assertTrue(e.getMessage().contains("已取消"));
    }

    @Test
    @DisplayName("超过 30 分钟未支付：自动关单并提示重新下单")
    void payTimeoutClosesOrder() {
        order.setCreateTime(LocalDateTime.now().minusMinutes(31));
        BizException e = assertThrows(BizException.class, () -> orderService.pay(1L, "lixinyang", "ALIPAY"));
        assertTrue(e.getMessage().contains("未支付"));
        assertEquals("已取消", order.getStatus());
        verify(orderRepo).save(order);
    }

    // ================= 退订 / 审批 =================

    @Test
    @DisplayName("未支付订单退订：直接取消，不用管理员审批")
    void cancelUnpaidDirectly() {
        Order r = orderService.applyCancel(1L, "lixinyang");
        assertEquals("已取消", r.getStatus());
        assertNotEquals("PENDING", r.getChangeStatus());
    }

    @Test
    @DisplayName("已支付订单退订：进入待审核状态")
    void cancelPaidNeedsApproval() {
        order.setPayStatus("PAID");
        Order r = orderService.applyCancel(1L, "lixinyang");
        assertEquals("已预订", r.getStatus(), "审批前订单不能直接取消");
        assertEquals("PENDING", r.getChangeStatus());
        assertEquals("CANCEL", r.getRequestType());
    }

    @Test
    @DisplayName("退订通过：订单取消并自动退款（沙箱）")
    void approveCancelRefunds() {
        order.setPayStatus("PAID");
        order.setChangeStatus("PENDING");
        order.setRequestType("CANCEL");

        Order r = orderService.approveChange(1L);

        assertEquals("已取消", r.getStatus());
        assertEquals("APPROVED", r.getChangeStatus());
        assertEquals("REFUNDED", r.getRefundStatus());
        assertNotNull(r.getRefundTime());
        assertTrue(r.getRefundNo().startsWith("RF"));
    }

    @Test
    @DisplayName("退订被驳回：状态回 REJECTED，订单不受影响")
    void rejectCancel() {
        order.setPayStatus("PAID");
        order.setChangeStatus("PENDING");
        order.setRequestType("CANCEL");

        Order r = orderService.rejectChange(1L);

        assertEquals("REJECTED", r.getChangeStatus());
        assertEquals("已预订", r.getStatus());
        assertNull(r.getRefundStatus());
    }

    @Test
    @DisplayName("重复审批：第二次直接拦下来，不会把数据改乱")
    void cannotApproveTwice() {
        order.setChangeStatus("APPROVED");
        BizException e = assertThrows(BizException.class, () -> orderService.approveChange(1L));
        assertTrue(e.getMessage().contains("已处理"));
        assertThrows(BizException.class, () -> orderService.rejectChange(1L));
    }

    @Test
    @DisplayName("已有待审核申请时，不能重复提交申请")
    void cannotApplyTwice() {
        order.setPayStatus("PAID");
        order.setChangeStatus("PENDING");
        assertThrows(BizException.class, () -> orderService.applyCancel(1L, "lixinyang"));
        assertThrows(BizException.class, () -> orderService.applyChange(1L, new Order()));
    }

    @Test
    @DisplayName("未支付订单不能申请改签")
    void cannotChangeUnpaidOrder() {
        BizException e = assertThrows(BizException.class, () -> orderService.applyChange(1L, new Order()));
        assertTrue(e.getMessage().contains("未支付"));
    }

    // ================= 权限 =================

    @Test
    @DisplayName("不能操作别人的订单")
    void cannotTouchOthersOrder() {
        BizException e = assertThrows(BizException.class, () -> orderService.applyCancel(1L, "someone-else"));
        assertEquals(403, e.getCode());
    }

    @Test
    @DisplayName("不存在的订单返回 404 业务码")
    void orderNotFound() {
        when(orderRepo.findById(99L)).thenReturn(Optional.empty());
        BizException e = assertThrows(BizException.class, () -> orderService.pay(99L, "lixinyang", "ALIPAY"));
        assertEquals(404, e.getCode());
    }

    // ================= 下单校验 =================

    @Test
    @DisplayName("下单：金额不能为负，默认状态为已预订 / 未支付")
    void createOrder() {
        Order req = new Order();
        req.setPrice(100.0);
        req.setUsername("lixinyang");
        Order saved = orderService.create(req);
        assertEquals("已预订", saved.getStatus());
        assertEquals("UNPAID", saved.getPayStatus());
        assertNotNull(saved.getOrderNo());

        Order bad = new Order();
        bad.setPrice(-1.0);
        assertThrows(BizException.class, () -> orderService.create(bad));
    }
}
