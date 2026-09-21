package com.lixinyang.travelassistant.vo;

import lombok.Data;

@Data
public class PlaceItem {
    private String name;
    private String address;
    private String distance;
    private String tel;
    private String type;
    private String location;
    private String rating;
    private String city;
    private String cost;      // 人均/参考价（高德部分 POI 提供）
    private String typeName;
    private String image;     // 图片地址（高德 POI photos[0].url）
}
