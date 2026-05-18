package com.regionalai.floatingball.server.modules.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.security.entity.SecurityRejectionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SecurityRejectionLogMapper extends BaseMapper<SecurityRejectionLog> {
}
