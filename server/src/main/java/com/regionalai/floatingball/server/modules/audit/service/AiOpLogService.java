package com.regionalai.floatingball.server.modules.audit.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.regionalai.floatingball.server.modules.audit.entity.AiOpLog;
import com.regionalai.floatingball.server.modules.audit.mapper.AiOpLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AiOpLogService extends ServiceImpl<AiOpLogMapper, AiOpLog> {
}
