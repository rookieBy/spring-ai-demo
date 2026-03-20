package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.business.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * User Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
