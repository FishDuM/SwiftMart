package hk.ljx.swiftmart.user.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import hk.ljx.swiftmart.common.domain.dataobject.UserDO;
import hk.ljx.swiftmart.common.domain.mapper.UserDOMapper;
import hk.ljx.swiftmart.common.enums.ResponseCodeEnum;
import hk.ljx.swiftmart.common.exception.BizException;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.user.enums.LoginTypeEnum;
import hk.ljx.swiftmart.user.enums.UserStatusEnum;
import hk.ljx.swiftmart.user.enums.VerifyCodeTypeEnum;
import hk.ljx.swiftmart.user.modal.vo.LoginUserRspVO;
import hk.ljx.swiftmart.user.modal.vo.RegisterUserReqVO;
import hk.ljx.swiftmart.user.modal.vo.SendVerifyCodeReqVO;
import hk.ljx.swiftmart.user.modal.vo.UserLoginReqVO;
import hk.ljx.swiftmart.user.service.UserService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserDOMapper userDOMapper;

    @Qualifier("bizExecutor")
    @Resource
    private Executor bizExecutor;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ImageCaptchaApplication imageCaptchaApplication;

    // 验证码过期时间 Lua 脚本
    private final DefaultRedisScript<Long> checkAndDeleteVerifyCodeScript;
    // 登录录失败计数 Lua 脚本
    private final DefaultRedisScript<Long> checkAndIncrementLoginFailScript;
    // 发送验证码原子 Lua 脚本
    private final DefaultRedisScript<Long> checkAndIncrementDailyLimitScript;

    public UserServiceImpl() {
        checkAndDeleteVerifyCodeScript = new DefaultRedisScript<>();
        checkAndDeleteVerifyCodeScript.setLocation(new ClassPathResource("lua/check_and_delete_verify_code.lua"));
        checkAndDeleteVerifyCodeScript.setResultType(Long.class);

        checkAndIncrementLoginFailScript = new DefaultRedisScript<>();
        checkAndIncrementLoginFailScript.setLocation(new ClassPathResource("lua/check_and_increment_login_fail_count.lua"));
        checkAndIncrementLoginFailScript.setResultType(Long.class);

        checkAndIncrementDailyLimitScript = new DefaultRedisScript<>();
        checkAndIncrementDailyLimitScript.setLocation(new ClassPathResource("lua/check_and_increment_verify_code_daily_limit.lua"));
        checkAndIncrementDailyLimitScript.setResultType(Long.class);
    }

    // BCrypt 密码编码器
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    // Redis 中验证码的 Key 前缀
    private static final String VERIFY_CODE_KEY_PREFIX = "verify_code:";
    // Redis 中发送频率限制的 Key 前缀
    private static final String VERIFY_CODE_LIMIT_KEY_PREFIX = "verify_code_limit:";
    // 验证码过期时间（分钟）
    private static final Long VERIFY_CODE_EXPIRE_MINUTES = 5L;
    // 发送频率限制时间（秒）
    private static final Long VERIFY_CODE_LIMIT_SECONDS = 60L;
    // Redis 中每日发送次数限制的 Key 前缀
    private static final String VERIFY_CODE_DAILY_LIMIT_KEY_PREFIX = "verify_code_daily:";
    // 每日发送次数上限
    private static final Integer VERIFY_CODE_DAILY_LIMIT = 10;
    // Redis 中登录失败次数的 Key 前缀
    private static final String LOGIN_FAIL_COUNT_KEY_PREFIX = "login_fail_count:";
    // 登录失败次数上限（超过此值则临时锁定账号）
    private static final Integer LOGIN_FAIL_MAX_COUNT = 5;
    // 账号临时锁定时间（分钟）
    private static final Long LOGIN_LOCK_MINUTES = 30L;

    /**
     * 用户注册
     * @param registerUserReqVO
     * @return
     */
    @Override
    public Response<?> register(RegisterUserReqVO registerUserReqVO) {
        String mobile = registerUserReqVO.getMobile();
        String password = registerUserReqVO.getPassword();
        String verifyCode = registerUserReqVO.getVerifyCode();

        // 1、校验验证码
        checkVerifyCode(mobile, verifyCode, VerifyCodeTypeEnum.REGISTER.getPurpose());
        // 2、校验手机号是否已经注册
        Long id = userDOMapper.selectIdByMobile(mobile);
        if (Objects.nonNull(id)) {
            throw new BizException(ResponseCodeEnum.USER_MOBILE_EXISTS);
        }
        // 3、密码加密
        String encodedPassword = PASSWORD_ENCODER.encode(password);
        // 4、注册用户
        UserDO userDO = UserDO.builder()
                .mobile(mobile)
                .password(encodedPassword)
                .nickname(generateNickname())
                .status(UserStatusEnum.ENABLE.getCode())
                .build();
        userDOMapper.insertSelective(userDO);
        stringRedisTemplate.delete(VERIFY_CODE_KEY_PREFIX + VerifyCodeTypeEnum.REGISTER.getPurpose() + ":" + mobile);
        log.info("用户注册成功，用户ID：{}，用户手机号：{}", userDO.getId(), mobile);
        return Response.success();
    }

    @Override
    public Response<LoginUserRspVO> login(UserLoginReqVO userLoginReqVO) {
        String mobile = userLoginReqVO.getMobile();
        Integer type = userLoginReqVO.getType();
        String password = userLoginReqVO.getPassword();
        String verifyCode = userLoginReqVO.getVerifyCode();
        // 校验当前手机号登录失败次数
        UserDO userDO = userDOMapper.selectByMobile(mobile);
        if (Objects.isNull(userDO)) {
            if (Objects.equals(type, LoginTypeEnum.PASSWORD.getCode())) {
                throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
            }
            throw new BizException(ResponseCodeEnum.USER_MOBILE_NOT_REGISTERED);
        }

        // 判断用户是否被禁用
        if (Objects.equals(userDO.getStatus(), UserStatusEnum.DISABLE.getCode())) {
            throw new BizException(ResponseCodeEnum.USER_STATUS_DISABLED);
        }

        if (type.equals(LoginTypeEnum.PASSWORD.getCode())) {
            // 密码登录
            checkLoginFailLimit(mobile);
            checkPassword(userDO.getPassword(), password, mobile);
        } else {
            // 验证码登录
            checkVerifyCode(mobile, verifyCode, VerifyCodeTypeEnum.LOGIN.getPurpose());
        }

        // 调用 sa-token
        StpUtil.login(userDO.getId());
        String token = StpUtil.getTokenValue();
        // 返回用户信息
        LoginUserRspVO userRspVO = LoginUserRspVO.builder()
                .token(token)
                .userInfo(LoginUserRspVO.UserInfo.builder()
                        .id(userDO.getId())
                        .nickname(userDO.getNickname())
                        .avatar(userDO.getAvatar())
                        .build()
                ).build();
        return Response.success(userRspVO);
    }

    @Override
    public Response<?> sendVerifyCode(SendVerifyCodeReqVO sendVerifyCodeReqVO) {
        String mobile = sendVerifyCodeReqVO.getMobile();
        Integer type = sendVerifyCodeReqVO.getType();
        String captchaId = sendVerifyCodeReqVO.getCaptchaId();

        if (StringUtils.isBlank(captchaId)) {
            throw new BizException(ResponseCodeEnum.CAPTCHA_VERIFICATION_FAILED);
        }

        // 判断 ImageCaptchaApplication 是否支持二次校验
        boolean verified = false;
        if (imageCaptchaApplication instanceof SecondaryVerificationApplication) {
            verified = ((SecondaryVerificationApplication) imageCaptchaApplication).secondaryVerification(captchaId);
        }
        if (!verified) {
            throw new BizException(ResponseCodeEnum.CAPTCHA_VERIFICATION_FAILED);
        }

        // 判断验证码类型是否合法
        VerifyCodeTypeEnum verifyCodeType = VerifyCodeTypeEnum.valueOf(type);
        if (Objects.isNull(verifyCodeType)) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_TYPE_ERROR);
        }

        String limitKey = VERIFY_CODE_LIMIT_KEY_PREFIX + verifyCodeType.getPurpose() + ":" + mobile;
        Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(limitKey, "1", VERIFY_CODE_LIMIT_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(absent)) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_SEND_TOO_FREQUENT);
        }

        String maxLimitKey = VERIFY_CODE_DAILY_LIMIT_KEY_PREFIX + verifyCodeType.getPurpose() + ":" + mobile + ":" + LocalDate.now();

        long secondsUntilMidnight = Duration.between(
                LocalDateTime.now(),
                LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
        ).getSeconds();
        // 设置同一号码同一业务类型一天验证码限制
        Long count = stringRedisTemplate.execute(checkAndIncrementDailyLimitScript,
                Collections.singletonList(maxLimitKey),
                String.valueOf(VERIFY_CODE_DAILY_LIMIT),
                String.valueOf(secondsUntilMidnight)
        );
        if (Objects.nonNull(count) && count == -1) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_DAILY_LIMIT_EXCEEDED);
        }

        String key = VERIFY_CODE_KEY_PREFIX + verifyCodeType.getPurpose() + ":" + mobile;
        String verifyCode = RandomUtil.randomNumbers(6);
        stringRedisTemplate.executePipelined(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations redisOperations) {
                redisOperations.opsForValue().setIfAbsent(limitKey, "1", VERIFY_CODE_LIMIT_SECONDS, TimeUnit.SECONDS);
                redisOperations.opsForValue().setIfAbsent(key, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                return null;
            }
        });
        // 异步发送验证码
        bizExecutor.execute(() -> sendSms(mobile, verifyCode));
        return Response.success();
    }

    @Override
    public Response<?> logout() {
        String token = StpUtil.getTokenValue();
        Object loginId = StpUtil.getLoginId();
        StpUtil.logout();
        log.info("用户{}退出登录，token为{}", loginId, token);
        return Response.success();
    }

    /**
     * 发送验证码
     * @param mobile 手机号
     * @param verifyCode 验证码
     */
    private void sendSms(String mobile, String verifyCode) {
        try {
            // TODO: 发送短信
            log.info("发送短信到{}，验证码为{}", mobile, verifyCode);
        } catch (Exception e) {
            log.error("发送短信失败，手机号为{}", mobile);
        }
    }

    /**
     * 校验验证码
     * @param verifyCode 验证码
     */
    private void checkVerifyCode(String mobile, String verifyCode, String purpose) {
        if (StringUtils.isBlank(verifyCode)) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }
        String key = VERIFY_CODE_KEY_PREFIX + purpose + ":" + mobile;
        Long result = stringRedisTemplate.execute(checkAndDeleteVerifyCodeScript,
                Collections.singletonList(key),
                verifyCode);
        if (result == null || result == 0) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }
    }

    /**
     * 校验登录失败次数
     * @param mobile 手机号
     */
    private void checkLoginFailLimit(String mobile) {
        // 判断当前号码输入错误次数是否大于 5
        String errorCount =  stringRedisTemplate.opsForValue().get(LOGIN_FAIL_COUNT_KEY_PREFIX + ":" + mobile);
        if (Objects.nonNull(errorCount)){
            int count = Integer.parseInt(errorCount);
            if (count >= LOGIN_FAIL_MAX_COUNT){
                throw new BizException(ResponseCodeEnum.LOGIN_FAIL_TOO_MANY);
            }
        }
    }

    /**
     * 增加登录失败次数
     * @param mobile 手机号
     */
    private void addLoginFailCount(String mobile) {
        String key = LOGIN_FAIL_COUNT_KEY_PREFIX + ":" + mobile;
        Long count = stringRedisTemplate.execute(checkAndIncrementLoginFailScript,
                Collections.singletonList(key),
                String.valueOf(LOGIN_FAIL_MAX_COUNT),
                String.valueOf(LOGIN_LOCK_MINUTES * 60));
        if (count == -1) {
            throw new BizException(ResponseCodeEnum.LOGIN_FAIL_TOO_MANY);
        }
    }

    /**
     * 校验原始密码和加密后的密码
     * @param truePassword 加密后的密码
     * @param password 原始密码
     */
    private void checkPassword(String truePassword, String password, String mobile) {
        if (StringUtils.isBlank(password)) {
            addLoginFailCount(mobile);
            throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
        }
        boolean matches = PASSWORD_ENCODER.matches(password, truePassword);
        if (!matches) {
            addLoginFailCount(mobile);
            throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
        }
        stringRedisTemplate.delete(LOGIN_FAIL_COUNT_KEY_PREFIX + ":" + mobile);
    }

    /**
     * 生成随机昵称
     * @return 用户_6位数字
     */
    private String generateNickname() {
        return "用户_" + RandomUtil.randomNumbers(6);
    }
}
