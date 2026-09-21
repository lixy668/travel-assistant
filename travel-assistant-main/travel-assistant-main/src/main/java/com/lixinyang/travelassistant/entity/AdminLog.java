package com.lixinyang.travelassistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理员操作审计日志：谁在什么时候改了什么 */
@Data
@Entity
@Table(name = "t_admin_log")
public class AdminLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String adminUsername;

    @Column(length = 30)
    private String action;

    @Column(length = 80)
    private String target;

    @Column(length = 300)
    private String detail;

    private LocalDateTime createTime = LocalDateTime.now();
}
