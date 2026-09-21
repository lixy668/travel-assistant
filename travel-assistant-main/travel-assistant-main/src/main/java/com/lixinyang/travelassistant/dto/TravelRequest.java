package com.lixinyang.travelassistant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;

@Data
public class TravelRequest {
    @NotNull(message = "城市不能为空")
    private String city;
    @NotNull(message = "天数最不能为空")
    @Min(value=1,message = "天数最少为一天")
    @Max(value=30,message = "天数最大为30天")
    private Integer days;
    @NotNull(message = "预算最不能为空")
    @DecimalMin(value="100",message = "预算最少为100元")
    private double budget;
}
