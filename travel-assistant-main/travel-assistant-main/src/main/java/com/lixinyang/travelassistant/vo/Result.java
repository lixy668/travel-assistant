package com.lixinyang.travelassistant.vo;
import lombok.Data;
@Data
public class Result<T> {
    private Boolean success;
    private Integer code;
    private String msg;
    private T data;
    private String error;
    private String rawResponse;
    public static <T> Result<T> ok() {
        Result<T> r = new Result<>();
        r.setSuccess(true); r.setCode(200); r.setMsg("成功");
        return r;
    }
    public static <T> Result<T> ok(T data) {
        Result<T> r = ok(); r.setData(data); return r;
    }
    public static <T> Result<T> fail() {
        Result<T> r = new Result<>();
        r.setSuccess(false); r.setCode(500); r.setMsg("失败");
        return r;
    }
    public static <T> Result<T> fail(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.setSuccess(false); r.setCode(code); r.setMsg(msg);
        return r;
    }
    public static <T> Result<T> error(String error, String rawResponse) {
        Result<T> r = new Result<>();
        r.setSuccess(false); r.setError(error); r.setRawResponse(rawResponse);
        return r;
    }
}