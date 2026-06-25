package hk.ljx.swiftmart.user.service;

import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.user.modal.vo.LoginUserRspVO;
import hk.ljx.swiftmart.user.modal.vo.RegisterUserReqVO;
import hk.ljx.swiftmart.user.modal.vo.UserLoginReqVO;

public interface UserService {

    /**
     * 用户注册
     * @param registerUserReqVO
     * @return
     */
    Response<?> register(RegisterUserReqVO registerUserReqVO);

    /**
     * 用户注册
     *
     * @param userLoginReqVO
     * @return
     */
    Response<LoginUserRspVO> login(UserLoginReqVO userLoginReqVO);
}
