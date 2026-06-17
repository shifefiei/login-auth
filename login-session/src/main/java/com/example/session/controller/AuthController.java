package com.example.session.controller;

import com.example.session.common.Result;
import com.example.session.dto.AuthDtos.LoginRequest;
import com.example.session.dto.AuthDtos.RegisterRequest;
import com.example.session.dto.LoginUser;
import com.example.session.entity.User;
import com.example.session.interceptor.AuthInterceptor;
import com.example.session.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 注册即登录：创建用户后直接建立 Session。
     */
    @PostMapping("/register")
    public Result<LoginUser> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest request) {
        if (userService.existsByUsername(req.username())) {
            return Result.error(1001, "用户名已存在");
        }
        User user = userService.register(req.username(), req.password(), req.phone(), req.email());
        return Result.ok(buildSession(request, user));
    }

    /**
     * 登录：校验密码，成功后把敏感用户信息写入 Session（存 Redis）。
     */
    @PostMapping("/login")
    public Result<LoginUser> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        Optional<User> user = userService.authenticate(req.username(), req.password());
        if (user.isEmpty()) {
            return Result.error(1002, "用户名或密码错误");
        }
        return Result.ok(buildSession(request, user.get()));
    }

    /**
     * 登出：销毁 Session，Redis 中对应数据立即删除。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.ok();
    }

    private LoginUser buildSession(HttpServletRequest request, User user) {
        LoginUser loginUser = new LoginUser(
                user.getId(), user.getUsername(), user.getPhone(), user.getEmail());
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, loginUser);
        return loginUser;
    }
}
