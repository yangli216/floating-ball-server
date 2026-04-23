package com.regionalai.floatingball.server.modules.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.feedback.entity.AiFeedback;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiFeedbackMapper extends BaseMapper<AiFeedback> {
}
