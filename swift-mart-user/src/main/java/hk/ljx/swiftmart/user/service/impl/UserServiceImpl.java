package hk.ljx.swiftmart.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import hk.ljx.swiftmart.common.ResponseCodeEnum.ResponseCodeEnum;
import hk.ljx.swiftmart.common.domain.dataobject.UserDO;
import hk.ljx.swiftmart.common.domain.mapper.UserDOMapper;
import hk.ljx.swiftmart.common.exception.BizException;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.user.enums.LoginTypeEnum;
import hk.ljx.swiftmart.user.enums.UserStatusEnum;
import hk.ljx.swiftmart.user.modal.vo.LoginUserRspVO;
import hk.ljx.swiftmart.user.modal.vo.RegisterUserReqVO;
import hk.ljx.swiftmart.user.modal.vo.UserLoginReqVO;
import hk.ljx.swiftmart.user.service.UserService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    @Resource
    private UserDOMapper userDOMapper;

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
