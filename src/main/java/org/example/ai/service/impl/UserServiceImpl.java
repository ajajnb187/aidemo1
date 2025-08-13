package org.example.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai.dto.*;
import org.example.ai.entity.SysUser;
import org.example.ai.entity.SysUserStats;
import org.example.ai.mapper.SysUserMapper;
import org.example.ai.mapper.SysUserStatsMapper;
import org.example.ai.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户服务实现类 - 使用简单的Session管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysUserStatsMapper userStatsMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    
    // Token到用户ID的映射，用于支持移动端token认证
    private static final Map<String, Long> tokenUserMap = new ConcurrentHashMap<>();

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求: {}", request.getUsername());
        
        // 1. 查询用户
        SysUser user = getUserByUsername(request.getUsername());
        if (user == null) {
            log.warn("登录失败: 用户不存在 - {}", request.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败: 密码错误 - {}", request.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            log.warn("登录失败: 账户被禁用 - {}", request.getUsername());
            throw new RuntimeException("账户已被禁用");
        }

        // 4. 生成简单的token
        String token = UUID.randomUUID().toString().replace("-", "");
        
        // 5. 存储token映射（用于移动端）
        tokenUserMap.put(token, user.getId());
        log.debug("Token映射已保存: {} -> UserId: {}, 当前映射总数: {}", 
            token.substring(0, 10) + "...", user.getId(), tokenUserMap.size());
        
        // 6. 将用户信息存储到Session中（用于Web端）
        HttpSession session = getCurrentSession();
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("token", token);
        
        log.info("用户登录成功: {} -> Token: {}, SessionId: {}", 
            user.getUsername(), token.substring(0, 10) + "...", session.getId());

        // 6. 转换为VO并返回
        UserVO userVO = convertToVO(user);
        return new LoginResponse(token, userVO);
    }

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        log.info("用户注册请求: {}", request.getUsername());
        
        // 1. 检查用户名是否已存在
        if (getUserByUsername(request.getUsername()) != null) {
            log.warn("注册失败: 用户名已存在 - {}", request.getUsername());
            throw new RuntimeException("用户名已存在");
        }

        // 2. 检查手机号是否已存在（如果提供了手机号）
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            LambdaQueryWrapper<SysUser> phoneQuery = new LambdaQueryWrapper<>();
            phoneQuery.eq(SysUser::getPhone, request.getPhone());
            if (userMapper.selectOne(phoneQuery) != null) {
                log.warn("注册失败: 手机号已被注册 - {}", request.getPhone());
                throw new RuntimeException("手机号已被注册");
            }
        }

        // 3. 检查邮箱是否已存在（如果提供了邮箱）
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            LambdaQueryWrapper<SysUser> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(SysUser::getEmail, request.getEmail());
            if (userMapper.selectOne(emailQuery) != null) {
                log.warn("注册失败: 邮箱已被注册 - {}", request.getEmail());
                throw new RuntimeException("邮箱已被注册");
            }
        }

        // 4. 创建新用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAddress(request.getAddress()); // 设置用户地址
        user.setStatus(1); // 默认启用状态

        // 5. 保存用户
        userMapper.insert(user);
        log.info("用户创建成功: {} (ID: {})", user.getUsername(), user.getId());

        // 6. 初始化用户统计数据
        SysUserStats stats = new SysUserStats();
        stats.setUserId(user.getId());
        stats.setLoginCount(1); // 注册即算一次登录
        stats.setLastLoginTime(LocalDateTime.now());
        userStatsMapper.insert(stats);

        // 7. 自动登录新注册用户
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenUserMap.put(token, user.getId()); // 存储token映射
        
        HttpSession session = getCurrentSession();
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("token", token);
        
        log.info("用户注册并自动登录成功: {}", user.getUsername());

        return convertToVO(user);
    }

    @Override
    public UserVO getCurrentUser() {
        // 1. 优先从Token头获取用户ID（适用于移动端）
        Long userId = getUserIdFromToken();
        
        // 2. 如果Token方式失败，尝试从Session获取用户ID（适用于Web端）
        if (userId == null) {
            try {
                HttpSession session = getCurrentSession();
                userId = (Long) session.getAttribute("userId");
            } catch (Exception e) {
                log.debug("获取Session失败: {}", e.getMessage());
            }
        }
        
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        
        log.debug("获取当前用户信息: userId = {}", userId);

        // 3. 查询用户信息
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            log.error("用户数据不一致: userId = {} 但数据库中不存在该用户", userId);
            // 清除无效的登录状态（优先清除Token映射）
            String token = getTokenFromRequest();
            if (token != null) {
                tokenUserMap.remove(token);
            }
            try {
                HttpSession session = getCurrentSession();
                session.invalidate();
            } catch (Exception e) {
                log.debug("清除Session失败: {}", e.getMessage());
            }
            throw new RuntimeException("用户数据异常，请重新登录");
        }

        // 4. 检查用户状态
        if (user.getStatus() == 0) {
            log.warn("用户已被禁用: {}", user.getUsername());
            // 清除被禁用用户的登录状态（优先清除Token映射）
            String token = getTokenFromRequest();
            if (token != null) {
                tokenUserMap.remove(token);
            }
            try {
                HttpSession session = getCurrentSession();
                session.invalidate();
            } catch (Exception e) {
                log.debug("清除Session失败: {}", e.getMessage());
            }
            throw new RuntimeException("账户已被禁用");
        }

        log.debug("获取用户信息成功: {}", user.getUsername());
        return convertToVO(user);
    }

    @Override
    public void logout() {
        // 1. 清除Token映射
        String token = getTokenFromRequest();
        if (token != null) {
            Long userId = tokenUserMap.remove(token);
            if (userId != null) {
                log.info("用户Token登出成功: Token: {} -> UserId: {}", token.substring(0, 10) + "...", userId);
                return;
            }
        }
        
        // 2. 清除Session（兼容Web端）
        try {
            HttpSession session = getCurrentSession();
            Long userId = (Long) session.getAttribute("userId");
            String username = (String) session.getAttribute("username");
            
            if (userId == null) {
                log.info("用户未登录，无需登出");
                return;
            }
            
            session.invalidate();
            log.info("用户Session登出成功: {} (ID: {})", username != null ? username : "未知用户", userId);
        } catch (Exception e) {
            log.debug("Session登出失败: {}", e.getMessage());
        }
    }

    @Override
    public SysUser getUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
        query.eq(SysUser::getUsername, username);
        return userMapper.selectOne(query);
    }

    @Override
    public UserVO convertToVO(SysUser user) {
        if (user == null) {
            return null;
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        // 查询用户统计信息
        LambdaQueryWrapper<SysUserStats> statsQuery = new LambdaQueryWrapper<>();
        statsQuery.eq(SysUserStats::getUserId, user.getId());
        SysUserStats stats = userStatsMapper.selectOne(statsQuery);

        if (stats != null) {
            UserVO.UserStatsVO statsVO = new UserVO.UserStatsVO();
            statsVO.setLoginCount(stats.getLoginCount());
            statsVO.setLastLoginTime(stats.getLastLoginTime());
            userVO.setStats(statsVO);
        }

        return userVO;
    }
    
    /**
     * 获取当前HTTP Session
     */
    private HttpSession getCurrentSession() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new RuntimeException("无法获取HTTP请求上下文");
        }
        return attrs.getRequest().getSession();
    }
    
    /**
     * 从Token头获取用户ID
     */
    private Long getUserIdFromToken() {
        String token = getTokenFromRequest();
        log.debug("开始从Token头获取用户ID, Token: {}", token != null ? token.substring(0, 10) + "..." : "null");
        
        if (token != null) {
            Long userId = tokenUserMap.get(token);
            log.debug("Token映射查询结果: Token: {} -> UserId: {}, 当前映射总数: {}", 
                token.substring(0, 10) + "...", userId, tokenUserMap.size());
            
            if (userId != null) {
                log.debug("从Token头获取用户ID成功: Token: {} -> UserId: {}", token.substring(0, 10) + "...", userId);
                return userId;
            } else {
                log.warn("Token映射中未找到对应的用户ID: {}", token.substring(0, 10) + "...");
            }
        } else {
            log.debug("请求头中未找到satoken");
        }
        return null;
    }
    
    /**
     * 从请求头获取Token
     */
    private String getTokenFromRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String token = request.getHeader("satoken");
                if (token != null && !token.trim().isEmpty()) {
                    return token.trim();
                }
            }
        } catch (Exception e) {
            log.debug("从请求头获取Token失败: {}", e.getMessage());
        }
        return null;
    }
}
