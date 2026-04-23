package com.regionalai.floatingball.server.modules.datapackage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.datapackage.entity.AiDataPackage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiDataPackageMapper extends BaseMapper<AiDataPackage> {
}
