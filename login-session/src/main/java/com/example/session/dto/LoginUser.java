package com.example.session.dto;

import java.io.Serializable;

/**
 * 存入 Session 的登录用户信息（含手机号、邮箱等敏感字段）。
 * 这些数据保存在服务端 Redis Session 中，客户端只持有 SessionID（Cookie），看不到内容。
 * 必须可序列化才能写入 Redis。
 */
public class LoginUser implements Serializable {

    private Long id;
    private String username;
    private String phone;
    private String email;

    public LoginUser() {
    }

    public LoginUser(Long id, String username, String phone, String email) {
        this.id = id;
        this.username = username;
        this.phone = phone;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
