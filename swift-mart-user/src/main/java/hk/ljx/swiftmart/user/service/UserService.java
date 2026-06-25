package hk.ljx.swiftmart.user.service;

import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.user.modal.vo.RegisterUserReqVO;

public interface UserService {

    /**
     * 用户注册
     *
     * @param registerUserReqVO
     * @return
     */
    Response<?> register(RegisterUserReqVO registerUserReqVO);
}
