package com.genius.smartlight.controller.admin.auth;

import com.genius.smartlight.common.ApiResponse;
import com.genius.smartlight.service.auth.AuthService;
import com.genius.smartlight.vo.auth.LoginReqVO;
import com.genius.smartlight.vo.auth.LoginRespVO;
import com.genius.smartlight.vo.auth.RegisterReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证接口", description = "用户注册、登录、登录返回 token 与当前用户店铺信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "账号注册",
            description = "通过用户名、手机号、密码和确认密码创建账号。注册成功返回提示文本，业务失败时 code != 200 且 msg 给出失败原因。"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "注册接口统一返回 {code,msg,data}，data 为注册成功提示文本",
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
            description = "通过用户名和密码登录。成功时 data 返回 token、userId、username、storeId、storeName、storeConfigured 等字段；密码错误或账号不存在时 code != 200，不返回 token。"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "登录接口统一返回 {code,msg,data}，成功时 data 为登录结果",
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
