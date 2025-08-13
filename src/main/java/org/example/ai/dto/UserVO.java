package org.example.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息VO - 简化版
 */
@Data
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 用户地址（城市）
     */
    private String address;

    /**
     * 创建时间（加入时间）
     */
    private LocalDateTime createTime;

    /**
     * 用户统计信息
     */
    private UserStatsVO stats;

    @Data
    public static class UserStatsVO {
        /**
         * 登录次数
         */
        private Integer loginCount;

        /**
         * 最后登录时间
         */
        private LocalDateTime lastLoginTime;
    }
}






