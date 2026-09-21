package com.lixinyang.travelassistant.vo;
import lombok.Data;
import java.util.List;
@Data
public class TravelRecommend {
    private Boolean success;
    private String city;
    private String days;
    private double totalBudget;
    private String error;
    private List<DailyItinerary> dailyItinerary;
    private BudgetBreakdown budgetBreakdown;
    private List<String> tips;
    private List<String> warnings;
    private String rawResponse;
    private String rawJson;
    private Boolean degraded;

    @Data
    public static class DailyItinerary {
        private Integer day;
        private String date;
        private Timeslot morning;
        private Timeslot afternoon;
        private Timeslot evening;
    }
    @Data
    public static class Timeslot {
        private String spot;
        private String duration;
        private String ticket;
        private String transportation;
        private String description;
    }
    @Data
    public static class BudgetBreakdown {
        private double accommodation;
        private double food;
        private double transportation;
        private double tickets;
        private double other;
    }
}