package com.regionalai.floatingball.server.modules.prompt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.regionalai.floatingball.server.modules.prompt.dto.PromptDeltaVO;
import com.regionalai.floatingball.server.modules.prompt.entity.AiPrompt;
import com.regionalai.floatingball.server.modules.prompt.mapper.AiPromptMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PromptService {

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

    public String latestVisibleVersion(String orgId, String regionId) {
        return resolveLatestVersion(findVisiblePrompts(orgId, regionId));
    }

    private List<AiPrompt> findVisiblePrompts(String orgId, String regionId) {
        List<AiPrompt> prompts = aiPromptMapper.selectList(new LambdaQueryWrapper<AiPrompt>()
            .eq(AiPrompt::getFgActive, "1")
            .eq(AiPrompt::getSdStatus, "1")
            .and(q -> q.eq(StringUtils.hasText(orgId), AiPrompt::getIdOrg, orgId)
                .or()
                .isNull(AiPrompt::getIdOrg).eq(StringUtils.hasText(regionId), AiPrompt::getIdRegion, regionId)
                .or()
                .isNull(AiPrompt::getIdOrg).isNull(AiPrompt::getIdRegion))
            .orderByDesc(AiPrompt::getUpdateTime));
        prompts.sort((left, right) -> Integer.compare(score(right, orgId, regionId), score(left, orgId, regionId)));
        return prompts;
    }

    private String resolveLatestVersion(List<AiPrompt> prompts) {
        return prompts.isEmpty() ? "0" : prompts.get(0).getVersionNum();
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
