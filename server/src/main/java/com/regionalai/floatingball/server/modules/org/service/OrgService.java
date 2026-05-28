package com.regionalai.floatingball.server.modules.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.org.entity.AiOrg;
import com.regionalai.floatingball.server.modules.org.mapper.AiOrgMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrgService {

    private final AiOrgMapper aiOrgMapper;

    public OrgService(AiOrgMapper aiOrgMapper) {
        this.aiOrgMapper = aiOrgMapper;
    }

    public PageResponse<AiOrg> list(long current, long size, String keyword) {
        Page<AiOrg> page = new Page<AiOrg>(current, size);
        LambdaQueryWrapper<AiOrg> wrapper = new LambdaQueryWrapper<AiOrg>()
            .eq(AiOrg::getFgActive, "1")
            .orderByAsc(AiOrg::getSortOrder)
            .orderByDesc(AiOrg::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(AiOrg::getNaOrg, keyword).or().like(AiOrg::getCdOrg, keyword));
        }
        Page<AiOrg> result = aiOrgMapper.selectPage(page, wrapper);
        return new PageResponse<AiOrg>(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Transactional
    public AiOrg save(AiOrg org) {
        if (!StringUtils.hasText(org.getNaOrg())) {
            throw new BusinessException("机构名称不能为空");
        }
        if (!StringUtils.hasText(org.getFgActive())) {
            org.setFgActive("1");
        }
        if (!StringUtils.hasText(org.getSdStatus())) {
            org.setSdStatus("1");
        }
        try {
            aiOrgMapper.insert(org);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("机构编码已存在");
        }
        return org;
    }

    @Transactional
    public AiOrg update(String idOrg, AiOrg org) {
        org.setIdOrg(idOrg);
        try {
            aiOrgMapper.updateById(org);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("机构编码已存在");
        }
        return aiOrgMapper.selectById(idOrg);
    }

    @Transactional
    public void invalidate(String idOrg) {
        AiOrg org = aiOrgMapper.selectById(idOrg);
        if (org == null) {
            throw new BusinessException("机构不存在");
        }
        org.setFgActive("0");
        aiOrgMapper.updateById(org);
    }
}
