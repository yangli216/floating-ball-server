package com.regionalai.floatingball.server.modules.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.config.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiConfigMapper extends BaseMapper<AiConfig> {
}
