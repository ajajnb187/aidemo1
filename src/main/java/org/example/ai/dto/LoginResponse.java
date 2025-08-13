package org.example.ai.dto;

import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 用户信息
     */
    private UserVO user;

    public LoginResponse(String token, UserVO user) {
        this.token = token;
        this.user = user;
    }
}















