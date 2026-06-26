package hk.ljx.swiftmart.user.service.impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserDOMapper userDOMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Qualifier("bizExecutor")
    @Resource
    private Executor bizExecutor;

    // Redis 中验证码的 Key 前缀
    private static final String VERIFY_CODE_KEY_PREFIX = "verify_code:";
    // Redis 中发送频率限制的 Key 前缀
    private static final String VERIFY_CODE_LIMIT_KEY_PREFIX = "verify_code_limit:";
    // 验证码过期时间（分钟）
    private static final Long VERIFY_CODE_EXPIRE_MINUTES = 5L;
    // 发送频率限制时间（秒）
    private static final Long VERIFY_CODE_LIMIT_SECONDS = 60L;

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
        // todo
        if (!verifyCode.equals("123456")) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }
        // 2、校验手机号是否已经注册
        Long id = userDOMapper.selectIdByMobile(mobile);
        if (Objects.nonNull(id)) {
            throw new BizException(ResponseCodeEnum.USER_MOBILE_EXISTS);
        }
        // 3、密码加密
        String encodedPassword = new BCryptPasswordEncoder().encode(password);
        // 4、注册用户
        UserDO userDO = UserDO.builder()
                .mobile(mobile)
                .password(encodedPassword)
                .nickname(generateNickname())
                .status(UserStatusEnum.ENABLE.getCode())
                .build();
        userDOMapper.insertSelective(userDO);
        log.info("用户注册成功，用户ID：{}，用户手机号：{}", userDO.getId(), mobile);
        return Response.success();
    }

    @Override
    public Response<LoginUserRspVO> login(UserLoginReqVO userLoginReqVO) {
        String mobile = userLoginReqVO.getMobile();
        Integer type = userLoginReqVO.getType();
        String password = userLoginReqVO.getPassword();
        String verifyCode = userLoginReqVO.getVerifyCode();

        UserDO userDO = userDOMapper.selectByMobile(mobile);
        if (Objects.isNull(userDO)) {
            throw new BizException(ResponseCodeEnum.USER_MOBILE_NOT_REGISTERED);
        }

        if (type.equals(LoginTypeEnum.PASSWORD.getCode())) {
            // 密码登录
            checkPassword(userDO.getPassword(), password);
        } else {
            // 验证码登录
            checkVerifyCode(verifyCode);
        }
        // 判断用户是否被禁用
        if (Objects.equals(userDO.getStatus(), UserStatusEnum.DISABLE.getCode())) {
            throw new BizException(ResponseCodeEnum.USER_STATUS_DISABLED);
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

        VerifyCodeTypeEnum verifyCodeType = VerifyCodeTypeEnum.valueOf(type);
        if (Objects.isNull(verifyCodeType)) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }

        String limitKey = VERIFY_CODE_LIMIT_KEY_PREFIX + verifyCodeType.getPurpose() + ":" + mobile;
        if (redisTemplate.hasKey(limitKey)) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_SEND_TOO_FREQUENT);
        }

        String key = VERIFY_CODE_KEY_PREFIX + verifyCodeType.getPurpose() + mobile;
        String verifyCode = RandomUtil.randomNumbers(6);
        redisTemplate.executePipelined(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations redisOperations) {
                redisOperations.opsForValue().set(limitKey, "1", VERIFY_CODE_LIMIT_SECONDS, TimeUnit.SECONDS);
                redisOperations.opsForValue().set(key, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                return null;
            }
        });
        // 异步发送验证码
        bizExecutor.execute(() -> sendSms(mobile, verifyCode));
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
     * 验证码登录
     * @param verifyCode 验证码
     */
    private void checkVerifyCode(String verifyCode) {
        if (StringUtils.isBlank(verifyCode)) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }
        // 调用验证码服务
        if (!"123456".equals(verifyCode)) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }
    }

    /**
     * 校验原始密码和加密后的密码
     * @param truePassword 加密后的密码
     * @param password 原始密码
     */
    private void checkPassword(String truePassword, String password) {
        if (StringUtils.isBlank(password)) {
            throw new BizException(ResponseCodeEnum.USER_PASSWORD_ERROR);
        }
        boolean matches = new BCryptPasswordEncoder().matches(password, truePassword);
        if (!matches) {
            throw new BizException(ResponseCodeEnum.USER_PASSWORD_ERROR);
        }
    }

    /**
     * 生成随机昵称
     * @return 用户_6位数字
     */
    private String generateNickname() {
        return "用户_" + RandomUtil.randomNumbers(6);
    }
}
