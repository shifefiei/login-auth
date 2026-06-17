package com.example.session.controller;

import com.example.session.common.Result;
import com.example.session.dto.LoginUser;
import com.example.session.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 受保护接口：从 Session 读取当前登录用户（含敏感信息）。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    public Result<LoginUser> profile(HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        return Result.ok(user);
    }
}
