package hk.ljx.swiftmart.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import hk.ljx.swiftmart.common.ResponseCodeEnum.ResponseCodeEnum;
import hk.ljx.swiftmart.common.domain.dataobject.UserDO;
import hk.ljx.swiftmart.common.domain.mapper.UserDOMapper;
import hk.ljx.swiftmart.common.exception.BizException;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.user.enums.UserStatusEnum;
import hk.ljx.swiftmart.user.modal.vo.RegisterUserReqVO;
import hk.ljx.swiftmart.user.service.UserService;
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

    /**
     * 生成随机昵称
     * @return 用户_6位数字
     */
    private String generateNickname() {
        return "用户_" + RandomUtil.randomNumbers(6);
    }
}
