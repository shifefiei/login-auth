package com.example.session.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.session.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis Plus Mapper，继承 BaseMapper 即拥有基础 CRUD。
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {
}
