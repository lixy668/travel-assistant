package com.lixinyang.travelassistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息：下单、支付、审批结果、退款到账都会给用户推一条 */
@Data
@Entity
@Table(name = "t_notice")
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String username;

    @Column(length = 20)
    private String type;

    @Column(length = 80)
    private String title;

    @Column(length = 600)
    private String content;

    private Boolean readFlag = false;

    private LocalDateTime createTime = LocalDateTime.now();
}
