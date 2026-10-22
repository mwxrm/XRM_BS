package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.dto.UserResponse;
import com.xrm.xrm_bs.entity.User;
import com.xrm.xrm_bs.dto.LoginRequest;
import com.xrm.xrm_bs.dto.LoginResponse;
import com.xrm.xrm_bs.dto.RegisterRequest;
import com.xrm.xrm_bs.service.UserService;
import com.xrm.xrm_bs.service.VerificationCodeStore;
import com.xrm.xrm_bs.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationCodeStore verificationCodeStore;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        // 先校验数学验证码（不会删除 token）
        if (!verificationCodeStore.verifyMath(request.getMathToken(), request.getMathAnswer())) {
            throw new RuntimeException("人机验证失败，请刷新验证码");
        }
        // 再校验邮箱验证码（会删除邮箱验证码，防止重复注册）
        if (!verificationCodeStore.verifyEmailCode(request.getEmail(), request.getEmailCode())) {
            throw new RuntimeException("邮箱验证码错误或已过期");
        }


        try {
            User user = userService.registerUser(request);
            // 使用 UserResponse 而不是 User
            UserResponse userResponse = convertToUserResponse(user);
//            return ResponseEntity.ok(user);
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            logger.error("注册失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        logger.info("登录请求: 邮箱={}", loginRequest.getEmail());

        try {
            // 首先检查用户是否存在
            try {
                User user = userService.findByEmail(loginRequest.getEmail());
                logger.info("找到用户: {}", user.getUsername());
            } catch (Exception e) {
                logger.warn("邮箱 {} 不存在，尝试按用户名查找", loginRequest.getEmail());
            }

            // 验证用户凭据 - 使用邮箱作为用户名
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),  // 使用邮箱
                            loginRequest.getPassword()
                    )
            );

            // 设置安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取用户信息
            String usernameOrEmail = authentication.getName();
            User user;

            // 尝试按邮箱查找用户
            try {
                user = userService.findByEmail(usernameOrEmail);
            } catch (RuntimeException e) {
                // 如果邮箱找不到，尝试按用户名查找
                user = userService.findByUsername(usernameOrEmail);
            }

            // 生成JWT令牌
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            // 返回登录响应
            UserResponse userResponse = convertToUserResponse(user);
            LoginResponse response = new LoginResponse(token, "登录成功", userResponse);
            logger.info("登录成功: 用户={}, Token生成成功", user.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("登录失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("邮箱或密码错误: " + e.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        try {
            // 从安全上下文获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String usernameOrEmail = authentication.getName();
                User user;

                // 尝试按邮箱查找
                try {
                    user = userService.findByEmail(usernameOrEmail);
                } catch (RuntimeException e) {
                    // 如果邮箱找不到，尝试按用户名查找
                    user = userService.findByUsername(usernameOrEmail);
                }

                // 使用 UserResponse 而不是 User
                UserResponse userResponse = convertToUserResponse(user);
//                return ResponseEntity.ok(user);
                return ResponseEntity.ok(userResponse);
            } else {
                return ResponseEntity.status(401).body("用户未认证");
            }
        } catch (Exception e) {
            logger.error("获取用户信息失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("获取用户信息失败");
        }
    }

    // 辅助方法：将 User 转换为 UserResponse
    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
