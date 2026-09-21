package com.lixinyang.travelassistant.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class StreamDone {
    private Boolean done= true;
    public static StreamDone of(){
        return new StreamDone();
    }

}
