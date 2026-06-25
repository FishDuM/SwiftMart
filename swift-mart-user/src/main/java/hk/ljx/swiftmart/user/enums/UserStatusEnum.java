package hk.ljx.swiftmart.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    DISABLE(0, "禁用"),
    ENABLE(1, "启用"),
    ;

    // 状态码
    private final int code;

    // 状态描述
    private final String desc;
}
