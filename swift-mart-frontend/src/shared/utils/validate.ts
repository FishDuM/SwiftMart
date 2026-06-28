/**
 * 校验中国大陆手机号
 * 与后端 RegisterUserReqVO / UserLoginReqVO / SendVerifyCodeReqVO 的
 * @Pattern(regexp = "^1[3-9]\\d{9}$") 保持一致。
 */
export const isValidMobile = (mobile: string): boolean => /^1[3-9]\d{9}$/.test(mobile)