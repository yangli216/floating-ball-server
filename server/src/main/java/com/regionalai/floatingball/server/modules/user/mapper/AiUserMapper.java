package com.regionalai.floatingball.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.user.entity.AiUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiUserMapper extends BaseMapper<AiUser> {
}
