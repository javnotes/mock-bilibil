package com.imooc.bilibili.domain.exception;

/**
 * 异常的数据实体类：条件异常，根据条件抛出异常
 * @author luf
 * @date 2022/03/03 20:47
 **/
public class ConditionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String code; // 定制异常错误码

    public ConditionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ConditionException(String message) {
        super(message);
        this.code = "500";//通用错误码
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
