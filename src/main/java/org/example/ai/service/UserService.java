package org.example.ai.service;

import org.example.ai.dto.*;
import org.example.ai.entity.SysUser;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     * @param request 注册请求
     * @return 用户信息
     */
    UserVO register(RegisterRequest request);

    /**
     * 获取当前用户信息
     * @return 用户信息
     */
    UserVO getCurrentUser();

    /**
     * 用户登出
     */
    void logout();

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体
     */
    SysUser getUserByUsername(String username);

    /**
     * 转换用户实体为VO
     * @param user 用户实体
     * @return 用户VO
     */
    UserVO convertToVO(SysUser user);
}















