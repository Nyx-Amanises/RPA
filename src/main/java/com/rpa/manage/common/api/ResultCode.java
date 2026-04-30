package com.rpa.manage.common.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {
    SUCCESS(100200, "成功"),
    BAD_REQUEST(100400, "请求参数校验失败"),
    UNAUTHORIZED(100401, "身份认证失败或Token已过期"),
    FORBIDDEN(100403, "用户无权访问该资源"),
    NOT_FOUND(100404, "请求资源不存在"),
    BUSINESS_ERROR(100422, "业务处理失败"),
    INTERNAL_ERROR(100500, "系统内部异常");

    private final int code;
    private final String message;
}
