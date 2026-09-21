package com.lixinyang.travelassistant.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Entity @Table(name = "t_usage_log")
public class UsageLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String username;
    private String action;   // LOGIN / RECOMMEND / CHAT
    private LocalDateTime createTime = LocalDateTime.now();
}