package com.genius.smartlight.controller.admin.auth;

import com.genius.smartlight.common.ApiResponse;
import com.genius.smartlight.vo.auth.LoginReqVO;
import com.genius.smartlight.vo.auth.LoginRespVO;
import com.genius.smartlight.vo.auth.RegisterReqVO;
import com.genius.smartlight.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterReqVO reqVO) {
        authService.register(reqVO);
        return ApiResponse.success("注册成功");
    }

    @PostMapping("/login")
    public ApiResponse<LoginRespVO> login(@RequestBody LoginReqVO reqVO) {
        return ApiResponse.success(authService.login(reqVO));
    }
}