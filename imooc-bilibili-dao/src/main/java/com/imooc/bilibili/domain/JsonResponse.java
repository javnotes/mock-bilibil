package com.imooc.bilibili.domain;

/**
 * 返回结果 封装类
 * @author luf
 * @date 2022/03/03 19:38
 **/
public class JsonResponse<T> {

    private String code;
    private String msg;
    private T data;

    /**
     * 用于返回没有数据的结果
     */
    public JsonResponse(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 只要有数据可返回，就默认是成功状态
     */
    public JsonResponse(T data) {
        code = "0";//成功
        msg = "成功";
        this.data = data;
    }

    /**
     * 请求成功，但不需要返回数据
     */
    public static JsonResponse<String> success() {
        return new JsonResponse<>(null);
    }

    /**
     * 成功，返回数据(String，即Json格式)
     */
    public static JsonResponse<String> success(String data) {
        return new JsonResponse<>(data);
    }

    /**
     * 默认的失败返回，没有数据data
     */
    public static JsonResponse<String> fail() {
        return new JsonResponse<>("1", "失败");
    }

    /**
     * 指定错误码和错误信息的失败返回
     */
    public JsonResponse<String> fail(String code, String msg) {
        return new JsonResponse<>(code, msg);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
