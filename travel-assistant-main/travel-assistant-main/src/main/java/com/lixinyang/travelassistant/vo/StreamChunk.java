package com.lixinyang.travelassistant.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StreamChunk {
    private String type="chunk";
    private String content;
    public static StreamChunk of(String content){
        return  new StreamChunk("chunk",content);
    }
}
