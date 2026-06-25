package hk.ljx.swiftmart.common.ResponseCodeEnum;

import hk.ljx.swiftmart.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    USER_MOBILE_EXISTS("20001", "该手机号已注册"),
    USER_VERIFY_CODE_ERROR("20002", "验证码错误"),
    USER_MOBILE_NOT_REGISTERED("20003", "该手机号未注册"),
    USER_PASSWORD_ERROR("20004", "密码错误"),
    USER_STATUS_DISABLED("20005", "账号已被禁用，请联系管理员"),

    ;

    // 异常码
    private String errorCode;
    // 错误信息
    private String errorMessage;
}
