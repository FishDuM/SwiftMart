package hk.ljx.swiftmart.user.controller;

import hk.ljx.swiftmart.common.aspect.ApiOperationLog;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.user.modal.vo.LoginUserRspVO;
import hk.ljx.swiftmart.user.modal.vo.RegisterUserReqVO;
import hk.ljx.swiftmart.user.modal.vo.SendVerifyCodeReqVO;
import hk.ljx.swiftmart.user.modal.vo.UserLoginReqVO;
import hk.ljx.swiftmart.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @ApiOperationLog(description = "用户注册")
    @PostMapping("/register")
    public Response<?> register (@Validated @RequestBody RegisterUserReqVO registerUserReqVO) {
        return userService.register(registerUserReqVO);
    }

    @ApiOperationLog(description = "用户登录")
    @PostMapping("/login")
    public Response<LoginUserRspVO> login (@Validated @RequestBody UserLoginReqVO userLoginReqVO) {
        return userService.login(userLoginReqVO);
    }

    @ApiOperationLog(description = "发送验证码")
    @PostMapping("/code/send")
    public Response<?> sendVerifyCode (@Validated @RequestBody SendVerifyCodeReqVO sendVerifyCodeReqVO) {
        return userService.sendVerifyCode(sendVerifyCodeReqVO);
    }

    @ApiOperationLog(description = "退出登录")
    @PostMapping("/logout")
    public Response<?> logout () {
        return userService.logout();
    }
}


