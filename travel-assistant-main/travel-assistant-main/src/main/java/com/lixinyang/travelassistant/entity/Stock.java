package com.lixinyang.travelassistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存：按「酒店/景点 + 日期 + 房型/票种」一行记录。
 * 没给某个日期配库存 = 不限量（兼容老数据）；配了库存就按 total - sold 卡住。
 */
@Data
@Entity
@Table(name = "t_stock", uniqueConstraints = @UniqueConstraint(columnNames = { "placeId", "bizDate", "roomType" }))
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long placeId;

    @Column(length = 20)
    private String bizDate;      // 库存对应的日期 yyyy-MM-dd

    @Column(length = 60)
    private String roomType;     // 房型 / 票种

    private Integer total = 0;   // 总库存

    private Integer sold = 0;    // 已占用

    private Double price;        // 当天价格（可选，旺季节假日调价用）

    private LocalDateTime updateTime = LocalDateTime.now();
}
