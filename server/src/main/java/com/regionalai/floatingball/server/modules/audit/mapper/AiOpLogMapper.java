package com.regionalai.floatingball.server.modules.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.audit.entity.AiOpLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiOpLogMapper extends BaseMapper<AiOpLog> {
}
