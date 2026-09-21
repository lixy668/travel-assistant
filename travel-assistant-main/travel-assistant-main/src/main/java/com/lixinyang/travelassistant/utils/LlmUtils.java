package com.lixinyang.travelassistant.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.awt.*;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LlmUtils {
    private String apikeys;
    private String model;
    private String baseurl;
    private OkHttpClient client;
    private ObjectMapper objectMapper=new ObjectMapper();
    public LlmUtils(String apikeys,String model,String baseurl){
        this.apikeys=apikeys;
        this.baseurl=baseurl;
        this.model=model;
       this.client= new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(120,TimeUnit.SECONDS)
                .writeTimeout(60,TimeUnit.SECONDS)
               .proxy(null)
               .build();

    }
    //旅游推荐接口
    public String chat(String systemPrompt,String userPrompt){
        String requestBody=buildRequestBody(systemPrompt,userPrompt, false, true);
        Request request=new  Request.Builder()
                .url(baseurl + "/chat/completions")
                .addHeader("Content-Type","application/json")
                .addHeader("Authorization","Bearer "+ apikeys)
                .post(RequestBody.create(requestBody,MediaType.parse("application/json;charset=utf-8")))
                .build();
        try (Response response=client.newCall(request).execute()){
            if (!response.isSuccessful()){
                throw new IOException("unexpected code"+response.code());
            }
            String responseBody=response.body().string();
            return extractContent(responseBody);

        }catch (IOException e){
            throw new RuntimeException(e);
        }//模型返回数据处理


    }
    private String extractContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText();
        }
        return "";
    }
    public String buildRequestBody(String systemPrompt, String userPrompt, Boolean stream, boolean jsonMode) {
        StringBuilder str = new StringBuilder();
        str.append("{");
        str.append("\"model\":\"" + model + "\",");
        str.append("\"stream\":" + stream + ",");
        str.append("\"messages\":[");
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            str.append("{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},");
        }
        str.append("{\"role\":\"user\",\"content\":\"" + escapeJson(userPrompt) + "\"}");
        str.append("],");
        str.append("\"temperature\":0.7");
        if (jsonMode) {
        str.append(",\"response_format\":{\"type\":\"json_object\"}");
        }
        str.append("}");
        return str.toString();
    }
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")    // 转义反斜杠
                .replace("\"", "\\\"")    // 转义双引号
                .replace("\n", "\\n")     // 转义换行符
                .replace("\r", "\\r")     // 转义回车符
                .replace("\t", "\\t");    // 转义制表符
    }
    public  String chatStream(String systemPrompt, String userPrompt, Consumer<String>callback)throws IOException{
        String requestBody=buildRequestBody(systemPrompt,userPrompt,true, false);
        Request request=new  Request.Builder()
                .url(baseurl+"/chat/completions")
                .addHeader("Content-Type","application/json")
                .addHeader("Authorization","Bearer "+ apikeys)
                .addHeader("Accept","text/event-stream")
                .post(RequestBody.create(requestBody,MediaType.parse("application/json;charset=utf-8")))
                .build();
        StringBuilder fullContent=new StringBuilder();
        System.out.println(" [" + System.currentTimeMillis() + "] 开始发送请求给 DeepSeek...");
        try (Response response=client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LLM调用异常 " + response.code());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))){
            String line;
            while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        String content = parseStreamContent(data);
                        if (content != null && !content.isEmpty()) {
                            fullContent.append(content);
                            callback.accept(content);
                        }


                    }

            }
        }
        }
        return fullContent.toString();


    }
    private String parseStreamContent(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).path("delta");
                return delta.path("content").asText("");
            }
        } catch (Exception e) {
            System.out.println("解析流式数据失败: " + e.getMessage());
        }
        return null;
    }
}
