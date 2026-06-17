package com.example.session.controller;

import com.example.session.common.Result;
import com.example.session.dto.LoginUser;
import com.example.session.entity.User;
import com.example.session.interceptor.AuthInterceptor;
import com.example.session.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 受保护接口：获取当前登录用户信息。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public Result<LoginUser> profile(HttpSession session) {
        LoginUser sessionUser = (LoginUser) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        // 为了防止 Session 中缓存的手机号/邮箱等数据过期（脏读），这里用 ID 回表查最新数据
        Optional<User> userOpt = userService.findById(sessionUser.getId());
        if (userOpt.isEmpty()) {
            // 用户在 DB 中已被删除
            session.invalidate();
            return Result.error(1004, "用户不存在");
        }
        User user = userOpt.get();
        // 返回最新数据，也可选择同步更新 session 里的缓存
        LoginUser freshUser = new LoginUser(user.getId(), user.getUsername(), user.getPhone(), user.getEmail());
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, freshUser);
        return Result.ok(freshUser);
    }
}
