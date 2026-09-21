package com.lixinyang.travelassistant.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_place")
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String type;         // HOTEL / SPOT

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String city;

    @Column(length = 200)
    private String address;

    private Double price;        // 参考价（景点可为门票价）
    private String rating;       // 评分

    @Column(length = 300)
    private String image;        // 图片链接

    @Column(length = 200)
    private String tags;         // 标签：近地铁/亲子/免费...

    @Column(length = 1000)
    private String description;  // 简介

    private Integer status = 1;  // 1 上架 / 0 下架
    private Integer sort = 0;    // 排序权重（越大越靠前）

    private LocalDateTime createTime = LocalDateTime.now();
}
