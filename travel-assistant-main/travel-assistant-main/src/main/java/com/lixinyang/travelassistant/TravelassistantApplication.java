package com.lixinyang.travelassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TravelassistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelassistantApplication.class, args);
    }

}
