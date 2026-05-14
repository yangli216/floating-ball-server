package com.regionalai.floatingball.server.modules.prompt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.prompt.dto.PromptDeltaVO;
import com.regionalai.floatingball.server.modules.prompt.entity.AiPrompt;
import com.regionalai.floatingball.server.modules.prompt.mapper.AiPromptMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PromptService {

    private static final Logger log = LoggerFactory.getLogger(PromptService.class);

    private final AiPromptMapper aiPromptMapper;

    public PromptService(AiPromptMapper aiPromptMapper) {
        this.aiPromptMapper = aiPromptMapper;
    }

    public PromptDeltaVO getDelta(String orgId, String regionId, String version) {
        List<AiPrompt> visiblePrompts = findVisiblePrompts(orgId, regionId);
        String latestVersion = resolveLatestVersion(visiblePrompts);
        if (StringUtils.hasText(version) && version.equals(latestVersion)) {
            return new PromptDeltaVO(latestVersion, new ArrayList<PromptDeltaVO.RemotePrompt>());
        }

        Map<String, PromptDeltaVO.RemotePrompt> merged = new LinkedHashMap<String, PromptDeltaVO.RemotePrompt>();
        for (AiPrompt prompt : visiblePrompts) {
            if (!merged.containsKey(prompt.getCdPrompt())) {
                merged.put(prompt.getCdPrompt(), new PromptDeltaVO.RemotePrompt(
                    prompt.getCdPrompt(),
                    prompt.getSysPrompt(),
                    prompt.getUserTemplate(),
                    prompt.getVersionNum()
                ));
            }
        }
        return new PromptDeltaVO(latestVersion, new ArrayList<PromptDeltaVO.RemotePrompt>(merged.values()));
    }

    public PageResponse<AiPrompt> list(long current, long size, String keyword) {
        Page<AiPrompt> page = new Page<AiPrompt>(current, size);
        LambdaQueryWrapper<AiPrompt> wrapper = new LambdaQueryWrapper<AiPrompt>()
            .eq(AiPrompt::getFgActive, "1")
            .orderByDesc(AiPrompt::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(AiPrompt::getNaPrompt, keyword).or().like(AiPrompt::getCdPrompt, keyword));
        }
        Page<AiPrompt> result = aiPromptMapper.selectPage(page, wrapper);
        return new PageResponse<AiPrompt>(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    public AiPrompt save(AiPrompt prompt) {
        validatePrompt(prompt);
        if (!StringUtils.hasText(prompt.getFgActive())) {
            prompt.setFgActive("1");
        }
        if (!StringUtils.hasText(prompt.getSdStatus())) {
            prompt.setSdStatus("0");
        }
        aiPromptMapper.insert(prompt);
        log.info("prompt saved. idPrompt={}, cdPrompt={}", prompt.getIdPrompt(), prompt.getCdPrompt());
        return prompt;
    }

    public AiPrompt update(String idPrompt, AiPrompt prompt) {
        AiPrompt existing = aiPromptMapper.selectById(idPrompt);
        if (existing == null || !"1".equals(existing.getFgActive())) {
            throw new BusinessException("Prompt 不存在");
        }
        validatePrompt(prompt);
        existing.setCdPrompt(prompt.getCdPrompt());
        existing.setNaPrompt(prompt.getNaPrompt());
        existing.setSysPrompt(prompt.getSysPrompt());
        existing.setUserTemplate(prompt.getUserTemplate());
        existing.setVersionNum(prompt.getVersionNum());
        existing.setSdPromptType(prompt.getSdPromptType());
        existing.setSdStatus(StringUtils.hasText(prompt.getSdStatus()) ? prompt.getSdStatus() : existing.getSdStatus());
        existing.setIdOrg(prompt.getIdOrg());
        existing.setIdRegion(prompt.getIdRegion());
        aiPromptMapper.updateById(existing);
        log.info("prompt updated. idPrompt={}, cdPrompt={}", idPrompt, existing.getCdPrompt());
        return aiPromptMapper.selectById(idPrompt);
    }

    public void publish(String idPrompt) {
        AiPrompt prompt = aiPromptMapper.selectById(idPrompt);
        if (prompt == null) {
            throw new BusinessException("Prompt 不存在");
        }
        List<AiPrompt> sameScene = aiPromptMapper.selectList(new LambdaQueryWrapper<AiPrompt>()
            .eq(AiPrompt::getCdPrompt, prompt.getCdPrompt())
            .eq(AiPrompt::getFgActive, "1"));
        for (AiPrompt item : sameScene) {
            if (!item.getIdPrompt().equals(idPrompt) && "1".equals(item.getSdStatus())) {
                item.setSdStatus("2");
                aiPromptMapper.updateById(item);
            }
        }
        prompt.setSdStatus("1");
        aiPromptMapper.updateById(prompt);
        log.info("prompt published. idPrompt={}, cdPrompt={}", idPrompt, prompt.getCdPrompt());
    }

    public void archive(String idPrompt) {
        AiPrompt prompt = aiPromptMapper.selectById(idPrompt);
        if (prompt == null) {
            throw new BusinessException("Prompt 不存在");
        }
        prompt.setSdStatus("2");
        aiPromptMapper.updateById(prompt);
        log.info("prompt archived. idPrompt={}", idPrompt);
    }

    public void invalidate(String idPrompt) {
        AiPrompt prompt = aiPromptMapper.selectById(idPrompt);
        if (prompt == null) {
            throw new BusinessException("Prompt 不存在");
        }
        prompt.setFgActive("0");
        aiPromptMapper.updateById(prompt);
        log.info("prompt invalidated. idPrompt={}", idPrompt);
    }

    public String latestVisibleVersion(String orgId, String regionId) {
        return resolveLatestVersion(findVisiblePrompts(orgId, regionId));
    }

    private List<AiPrompt> findVisiblePrompts(String orgId, String regionId) {
        List<AiPrompt> prompts = aiPromptMapper.selectList(new LambdaQueryWrapper<AiPrompt>()
            .eq(AiPrompt::getFgActive, "1")
            .eq(AiPrompt::getSdStatus, "1")
            .and(q -> q.eq(StringUtils.hasText(orgId), AiPrompt::getIdOrg, orgId)
                .or()
                .eq(StringUtils.hasText(regionId), AiPrompt::getIdRegion, regionId)
                .or()
                .isNull(AiPrompt::getIdOrg).isNull(AiPrompt::getIdRegion))
            .orderByDesc(AiPrompt::getUpdateTime));
        prompts.sort((left, right) -> Integer.compare(score(right, orgId, regionId), score(left, orgId, regionId)));
        return prompts;
    }

    private String resolveLatestVersion(List<AiPrompt> prompts) {
        return prompts.isEmpty() ? "0" : prompts.get(0).getVersionNum();
    }

    private void validatePrompt(AiPrompt prompt) {
        if (prompt == null) {
            throw new BusinessException("请求体不能为空");
        }
        if (!StringUtils.hasText(prompt.getNaPrompt()) || !StringUtils.hasText(prompt.getCdPrompt())) {
            throw new BusinessException("Prompt 名称和场景编码不能为空");
        }
        if (!StringUtils.hasText(prompt.getVersionNum())) {
            throw new BusinessException("Prompt 版本号不能为空");
        }
    }

    private int score(AiPrompt prompt, String orgId, String regionId) {
        if (StringUtils.hasText(orgId) && orgId.equals(prompt.getIdOrg())) {
            return 3;
        }
        if (StringUtils.hasText(regionId) && regionId.equals(prompt.getIdRegion())) {
            return 2;
        }
        return 1;
    }
}
