package com.lixinyang.travelassistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 保存下来的 AI 行程规划结果，用户之后能回看 */
@Data
@Entity
@Table(name = "t_trip_plan")
public class TripPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String username;

    @Column(length = 60)
    private String city;

    private Integer days;

    private Double budget;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String resultJson;

    private LocalDateTime createTime = LocalDateTime.now();
}
