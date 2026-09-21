package com.lixinyang.travelassistant.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Streamerror {
    private String error;
    public static Streamerror of(String error){
        return new Streamerror(error);

    }
}
