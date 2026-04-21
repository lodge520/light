package com.genius.smartlight.controller.admin.auth;

import com.genius.smartlight.common.ApiResponse;
import com.genius.smartlight.vo.auth.LoginReqVO;
import com.genius.smartlight.vo.auth.LoginRespVO;
import com.genius.smartlight.vo.auth.RegisterReqVO;
import com.genius.smartlight.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "账号管理", description = "用户注册、登录相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "账号注册",
            description = "用户通过用户名、密码等信息完成账号注册"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "注册成功",
            content = @Content(schema = @Schema(implementation = String.class))
    )
    @PostMapping("/register")
    public ApiResponse<String> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "注册请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RegisterReqVO.class))
            )
            @Valid @RequestBody RegisterReqVO reqVO) {
        authService.register(reqVO);
        return ApiResponse.success("注册成功");
    }

    @Operation(
            summary = "账号登录",
            description = "用户通过用户名和密码登录，登录成功后返回 token、用户信息、店铺信息等"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "登录成功",
            content = @Content(schema = @Schema(implementation = LoginRespVO.class))
    )
    @PostMapping("/login")
    public ApiResponse<LoginRespVO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "登录请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginReqVO.class))
            )
            @Valid @RequestBody LoginReqVO reqVO) {
        return ApiResponse.success(authService.login(reqVO));
    }
}