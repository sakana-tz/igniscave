package com.evernight.base.entity.enums;


/**
 * 响应码枚举类
 */
public enum ResponseCodeEnum {
    // 成功相关
    CODE_200(200, "请求成功"),
    CODE_201(201, "创建成功"),
    CODE_204(204, "删除成功"),

    // 客户端错误相关
    CODE_400(400, "请求参数错误"),
    CODE_401(401, "未授权访问"),
    CODE_403(403, "禁止访问"),
    CODE_404(404, "请求地址不存在"),
    CODE_405(405, "请求方法不被允许"),
    CODE_408(408, "请求超时"),

    // 服务器错误相关
    CODE_500(500, "服务器返回错误"),
    CODE_501(501, "服务未实现"),
    CODE_502(502, "网关错误"),
    CODE_503(503, "服务不可用"),
    CODE_504(504, "网关超时"),

    // 业务相关错误
    CODE_600(600, "业务繁忙，请稍后再试"),
    CODE_601(601, "数据已存在"),
    CODE_602(602, "数据不存在"),
    CODE_603(603, "权限不足"),
    CODE_604(604, "账户已被禁用"),
    CODE_605(605, "验证码错误"),
    CODE_606(606, "账户或密码错误"),
    CODE_610(610, "文件上传异常"),
    CODE_611(611, "文件上传超出限制");


    private Integer code;

    private String msg;

    /**
     * 构造函数
     *
     * @param code 响应码
     * @param msg  响应信息
     */
    ResponseCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取响应码
     *
     * @return 响应码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取响应信息
     *
     * @return 响应信息
     */
    public String getMsg() {
        return msg;
    }

}
