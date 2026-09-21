package com.lixinyang.travelassistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 发票申请：用户在订单里申请，管理员在后台开票 */
@Data
@Entity
@Table(name = "t_invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String username;

    private Long orderId;

    @Column(length = 40)
    private String orderNo;

    private Double amount;

    @Column(length = 120)
    private String title;

    @Column(length = 60)
    private String taxNo;

    @Column(length = 80)
    private String email;

    @Column(length = 20)
    private String status;

    private LocalDateTime applyTime = LocalDateTime.now();
    private LocalDateTime issueTime;
}
