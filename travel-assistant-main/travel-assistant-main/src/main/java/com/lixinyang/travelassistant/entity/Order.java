package com.lixinyang.travelassistant.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 40)
    private String orderNo;      // 订单号

    @Column(length = 50)
    private String username;     // 下单用户

    @Column(length = 20)
    private String type;         // TRAIN / FLIGHT

    private String fromCity;     // 出发地
    private String toCity;       // 目的地
    private String travelDate;   // 出行日期
    private String departTime;   // 出发时间
    private String arriveTime;   // 到达时间
    private String seat;         // 座位/舱位
    private Double price;        // 价格

    @Column(length = 20)
    private String status;       // 已预订 / 已取消

    @Column(length = 20)
    private String payStatus;    // UNPAID / PAID（沙箱模拟支付）
    private LocalDateTime payTime;
    @Column(length = 20)
    private String payMethod;    // ALIPAY / WECHAT / UNIONPAY / CARD（沙箱）
    @Column(length = 40)
    private String payNo;        // 支付流水号（沙箱模拟生成）

    // ===== 退款（沙箱）=====
    @Column(length = 20)
    private String refundStatus;     // REFUNDED = 已退款
    @Column(length = 40)
    private String refundNo;         // 退款流水号
    private LocalDateTime refundTime;

    private String passenger;    // 乘客
    private String phone;        // 联系电话

    // ===== 库存关联（用于占用/回补真实库存）=====
    private Long placeId;        // 对应 t_place.id
    @Column(length = 20)
    private String bizDate;      // 库存日期（入住日 / 游玩日）
    @Column(length = 60)
    private String roomType;     // 房型 / 票种
    private Integer quantity = 1; // 数量（几间 / 几张）

    private LocalDateTime createTime = LocalDateTime.now(); // 下单时间

    // ===== 改签申请 =====
    @Column(length = 20)
    private String changeStatus;      // "" / PENDING / APPROVED / REJECTED
    @Column(length = 20)
    private String requestType;       // CHANGE(改签) / CANCEL(退订)
    private String reqFromCity;       // 申请改出发地
    private String reqToCity;         // 申请改目的地
    private String reqTravelDate;     // 申请改出行日期
    private String reqDepartTime;     // 申请改出发时间
    private String reqArriveTime;     // 申请改到达时间
    private String reqSeat;           // 申请改座位/舱位
    private Double reqPrice;          // 申请改价格
    private LocalDateTime changeApplyTime; // 申请时间
}
