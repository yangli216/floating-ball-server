package com.regionalai.floatingball.server.modules.prompt.service;

import com.regionalai.floatingball.server.modules.prompt.dto.PromptDeltaVO;
import com.regionalai.floatingball.server.modules.prompt.entity.AiPrompt;
import com.regionalai.floatingball.server.modules.prompt.mapper.AiPromptMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptServiceTest {

    @Mock
    private AiPromptMapper aiPromptMapper;

    private PromptService promptService;

    @BeforeEach
    void setUp() {
        promptService = new PromptService(aiPromptMapper);
    }

    @Test
    void getDeltaShouldPreferOrgPromptAndMergeBySceneCode() {
        AiPrompt globalDiagnosis = buildPrompt("P1", "diagnosis", "global", "1.0.0", null, null, "1");
        AiPrompt regionDiagnosis = buildPrompt("P2", "diagnosis", "region", "2.0.0", null, "REG001", "1");
        AiPrompt orgDiagnosis = buildPrompt("P3", "diagnosis", "org", "3.0.0", "ORG001", null, "1");
        AiPrompt triage = buildPrompt("P4", "triage", "triage", "1.0.0", null, null, "1");

        when(aiPromptMapper.selectList(any())).thenReturn(Arrays.asList(globalDiagnosis, regionDiagnosis, orgDiagnosis, triage));

        PromptDeltaVO delta = promptService.getDelta("ORG001", "REG001", null);

        assertEquals("3.0.0", delta.getVersion());
        assertEquals(2, delta.getPrompts().size());
        assertEquals("diagnosis", delta.getPrompts().get(0).getCdPrompt());
        assertEquals("org", delta.getPrompts().get(0).getSysPrompt());
        assertEquals("triage", delta.getPrompts().get(1).getCdPrompt());
    }

    @Test
    void getDeltaShouldReturnEmptyWhenVersionAlreadyLatest() {
        AiPrompt orgDiagnosis = buildPrompt("P3", "diagnosis", "org", "3.0.0", "ORG001", null, "1");
        when(aiPromptMapper.selectList(any())).thenReturn(Arrays.asList(orgDiagnosis));

        PromptDeltaVO delta = promptService.getDelta("ORG001", "REG001", "3.0.0");

        assertEquals("3.0.0", delta.getVersion());
        assertTrue(delta.getPrompts().isEmpty());
    }

    @Test
    void publishShouldArchiveOtherPublishedPromptsInSameScene() {
        AiPrompt target = buildPrompt("P1", "diagnosis", "draft", "3.0.0", "ORG001", null, "0");
        AiPrompt published = buildPrompt("P2", "diagnosis", "published", "2.0.0", "ORG001", null, "1");
        when(aiPromptMapper.selectById("P1")).thenReturn(target);
        when(aiPromptMapper.selectList(any())).thenReturn(Arrays.asList(target, published));

        promptService.publish("P1");

        ArgumentCaptor<AiPrompt> captor = ArgumentCaptor.forClass(AiPrompt.class);
        verify(aiPromptMapper, times(2)).updateById(captor.capture());
        List<AiPrompt> updated = captor.getAllValues();
        assertEquals("P2", updated.get(0).getIdPrompt());
        assertEquals("2", updated.get(0).getSdStatus());
        assertEquals("P1", updated.get(1).getIdPrompt());
        assertEquals("1", updated.get(1).getSdStatus());
    }

    private AiPrompt buildPrompt(String idPrompt,
                                 String cdPrompt,
                                 String sysPrompt,
                                 String versionNum,
                                 String idOrg,
                                 String idRegion,
                                 String sdStatus) {
        AiPrompt prompt = new AiPrompt();
        prompt.setIdPrompt(idPrompt);
        prompt.setCdPrompt(cdPrompt);
        prompt.setNaPrompt(cdPrompt + "-name");
        prompt.setSysPrompt(sysPrompt);
        prompt.setUserTemplate("{{input}}");
        prompt.setVersionNum(versionNum);
        prompt.setIdOrg(idOrg);
        prompt.setIdRegion(idRegion);
        prompt.setSdStatus(sdStatus);
        prompt.setFgActive("1");
        return prompt;
    }
}
