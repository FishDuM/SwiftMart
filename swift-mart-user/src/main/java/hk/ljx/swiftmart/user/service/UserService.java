package hk.ljx.swiftmart.user.service;

import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.user.modal.vo.LoginUserRspVO;
import hk.ljx.swiftmart.user.modal.vo.RegisterUserReqVO;
import hk.ljx.swiftmart.user.modal.vo.SendVerifyCodeReqVO;
import hk.ljx.swiftmart.user.modal.vo.UserLoginReqVO;

public interface UserService {

    /**
     * 用户注册
     * @param registerUserReqVO
     * @return
     */
    Response<?> register(RegisterUserReqVO registerUserReqVO);

    /**
     * 用户登录
     *
     * @param userLoginReqVO
     * @return
     */
    Response<LoginUserRspVO> login(UserLoginReqVO userLoginReqVO);

    /**
     * 发送验证码
     *
     * @param sendVerifyCodeReqVO
     * @return
     */
    Response<?> sendVerifyCode(SendVerifyCodeReqVO sendVerifyCodeReqVO);

    /**
     * 退出登录
     * @return
     */
    Response<?> logout();
}
