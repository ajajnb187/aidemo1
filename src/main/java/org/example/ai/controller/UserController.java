package org.example.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai.dto.*;
import org.example.ai.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

/**
 * 用户控制器 - 使用简单的Session管理
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户登录
     * 不需要鉴权，所有人都可以访问
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        log.info("用户登录成功: {}", request.getUsername());
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 用户注册  
     * 不需要鉴权，所有人都可以访问
     */
    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Validated @RequestBody RegisterRequest request) {
        UserVO user = userService.register(request);
        log.info("用户注册成功: {}", request.getUsername());
        return ApiResponse.success("注册成功", user);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public ApiResponse<UserVO> getCurrentUser() {
        log.info("收到获取用户信息请求");
        try {
            UserVO user = userService.getCurrentUser();
            log.info("获取用户信息成功: {}", user.getUsername());
            return ApiResponse.success("获取成功", user);
        } catch (Exception e) {
            log.warn("获取用户信息失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        userService.logout();
        return ApiResponse.success("登出成功", null);
    }
    
    /**
     * 获取当前登录状态
     * 工具方法，用于前端检查登录状态
     */
    @GetMapping("/status")
    public ApiResponse<Object> getLoginStatus(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        boolean isLogin = (userId != null);
        
        if (isLogin) {
            String username = (String) session.getAttribute("username");
            String token = (String) session.getAttribute("token");
            
            return ApiResponse.success("已登录", new Object() {
                public final boolean isLogin = true;
                public final Long userId = UserController.this.getUserIdFromSession(session);
                public final String username = this.username;
                public final String tokenValue = token != null ? token : "";
            });
        } else {
            return ApiResponse.success("未登录", new Object() {
                public final boolean isLogin = false;
            });
        }
    }
    
    // 辅助方法
    private String getUsername(String username) {
        return username != null ? username : "";
    }
    
    private Long getUserIdFromSession(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
}

