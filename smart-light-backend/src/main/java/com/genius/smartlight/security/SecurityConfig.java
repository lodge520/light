package com.genius.smartlight.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // 登录注册放行
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 设备端 WebSocket：必须放在 /ws/** 前面
                        .requestMatchers("/ws/device").permitAll()

                        // 设备主动上报接口：放行
                        .requestMatchers(HttpMethod.POST,
                                "/admin/device/announce",
                                "/admin/device/state-report",
                                "/admin/lux/create",
                                "/admin/duration/create"
                        ).permitAll()

                        // OTA 如果后面要用，也先放行
                        .requestMatchers(
                                "/ota/**"
                        ).permitAll()

                        // 浏览器端 websocket：继续走 JWT
                        .requestMatchers("/ws", "/ws/**").authenticated()

                        // 其余全部要求用户登录
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}