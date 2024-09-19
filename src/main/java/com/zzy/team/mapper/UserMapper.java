package com.zzy.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzy.team.model.domain.User;

import java.util.List;

/**
* @author zzy
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2024-05-07 13:20:58
* @Entity generator.domain.User
*/
public interface UserMapper extends BaseMapper<User> {
    User recomendUser();
}




