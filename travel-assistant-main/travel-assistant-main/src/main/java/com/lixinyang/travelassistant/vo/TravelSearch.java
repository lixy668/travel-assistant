package com.lixinyang.travelassistant.vo;

import lombok.Data;
import java.util.List;

@Data
public class TravelSearch {
    private String keyword;
    private Boolean success;
    private String error;
    private List<Place> attractions; // 景区 / 景点
    private List<Place> hotels;      // 附近酒店
    private List<Place> nearby;      // 附近景区 / 去处

    @Data
    public static class Place {
        private String name;
        private String city;
        private String ticket;
        private String price;
        private String rating;
        private String address;
        private String distance;
        private String tags;
        private String description;
    }
}
