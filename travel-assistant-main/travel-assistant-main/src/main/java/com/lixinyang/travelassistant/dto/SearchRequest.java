package com.lixinyang.travelassistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;
}
