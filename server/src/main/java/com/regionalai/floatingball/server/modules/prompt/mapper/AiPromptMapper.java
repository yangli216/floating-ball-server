package com.regionalai.floatingball.server.modules.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.prompt.entity.AiPrompt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiPromptMapper extends BaseMapper<AiPrompt> {
}
