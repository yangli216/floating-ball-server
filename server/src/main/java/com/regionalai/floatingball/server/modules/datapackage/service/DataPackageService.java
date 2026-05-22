package com.regionalai.floatingball.server.modules.datapackage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.datapackage.dto.MappingDeltaVO;
import com.regionalai.floatingball.server.modules.datapackage.dto.TemplateDeltaVO;
import com.regionalai.floatingball.server.modules.datapackage.entity.AiDataPackage;
import com.regionalai.floatingball.server.modules.datapackage.mapper.AiDataPackageMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DataPackageService {

    private static final String ACTIVE_ENABLED = "1";
    private static final String STATUS_PUBLISHED = "1";
    private static final String PACKAGE_TYPE_TEMPLATE = "template";
    private static final String PACKAGE_TYPE_MAPPING = "mapping";

    private final AiDataPackageMapper aiDataPackageMapper;
    private final ObjectMapper objectMapper;
    private final BuiltinTemplateSeedService builtinTemplateSeedService;

    public DataPackageService(AiDataPackageMapper aiDataPackageMapper,
                              ObjectMapper objectMapper,
                              BuiltinTemplateSeedService builtinTemplateSeedService) {
        this.aiDataPackageMapper = aiDataPackageMapper;
        this.objectMapper = objectMapper;
        this.builtinTemplateSeedService = builtinTemplateSeedService;
    }

    public TemplateDeltaVO getTemplateDelta(String orgId, String regionId, String version) {
        AiDataPackage pkg = findVisiblePackage(PACKAGE_TYPE_TEMPLATE, orgId, regionId);
        if (pkg == null) {
            return builtinTemplateSeedService.getDelta(version);
        }
        TemplateDeltaVO vo = new TemplateDeltaVO();
        vo.setVersion(pkg.getVersionNum());
        if (StringUtils.hasText(version) && version.equals(pkg.getVersionNum())) {
            return vo;
        }
        try {
            Map<String, Object> content = objectMapper.readValue(pkg.getContentJson(), new TypeReference<Map<String, Object>>() {});
            Object western = content.get("western");
            Object tcm = content.get("tcm");
            vo.setWestern(western instanceof List ? (List<Object>) western : Collections.<Object>emptyList());
            vo.setTcm(tcm instanceof List ? (List<Object>) tcm : Collections.<Object>emptyList());
            return vo;
        } catch (IOException ex) {
            throw new BusinessException("模板数据包 JSON 解析失败");
        }
    }

    public MappingDeltaVO getMappingDelta(String orgId, String regionId, String version) {
        AiDataPackage pkg = findVisiblePackage(PACKAGE_TYPE_MAPPING, orgId, regionId);
        MappingDeltaVO vo = new MappingDeltaVO();
        if (pkg == null) {
            vo.setVersion("0");
            return vo;
        }
        vo.setVersion(pkg.getVersionNum());
        if (StringUtils.hasText(version) && version.equals(pkg.getVersionNum())) {
            return vo;
        }
        try {
            Map<String, String> content = objectMapper.readValue(pkg.getContentJson(), new TypeReference<Map<String, String>>() {});
            vo.setDiagnoses(content.get("diagnoses"));
            vo.setMedicines(content.get("medicines"));
            vo.setItems(content.get("items"));
            vo.setTcmDiagnoses(content.get("tcmDiagnoses"));
            vo.setTcmSyndromes(content.get("tcmSyndromes"));
            vo.setTcmTreatments(content.get("tcmTreatments"));
            return vo;
        } catch (IOException ex) {
            throw new BusinessException("映射数据包 JSON 解析失败");
        }
    }

    public String latestVisibleVersion(String type, String orgId, String regionId) {
        AiDataPackage pkg = findVisiblePackage(type, orgId, regionId);
        if (pkg != null) {
            return pkg.getVersionNum();
        }
        if (PACKAGE_TYPE_TEMPLATE.equals(type)) {
            return builtinTemplateSeedService.getVersion();
        }
        return "0";
    }

    private AiDataPackage findVisiblePackage(String type, String orgId, String regionId) {
        LambdaQueryWrapper<AiDataPackage> wrapper = new LambdaQueryWrapper<AiDataPackage>()
            .eq(AiDataPackage::getFgActive, ACTIVE_ENABLED)
            .eq(AiDataPackage::getSdPackageType, type)
            .eq(AiDataPackage::getSdStatus, STATUS_PUBLISHED)
            .orderByDesc(AiDataPackage::getUpdateTime);
        appendVisibleScopeCondition(wrapper, orgId, regionId);
        List<AiDataPackage> packages = aiDataPackageMapper.selectList(wrapper);
        if (packages.isEmpty()) {
            return null;
        }
        packages.sort((left, right) -> Integer.compare(score(right, orgId, regionId), score(left, orgId, regionId)));
        return packages.get(0);
    }

    private void appendVisibleScopeCondition(LambdaQueryWrapper<AiDataPackage> wrapper, String orgId, String regionId) {
        final boolean hasOrgId = StringUtils.hasText(orgId);
        final boolean hasRegionId = StringUtils.hasText(regionId);
        wrapper.and(q -> {
            if (hasOrgId) {
                q.eq(AiDataPackage::getIdOrg, orgId);
                if (hasRegionId) {
                    q.or(inner -> inner.isNull(AiDataPackage::getIdOrg).eq(AiDataPackage::getIdRegion, regionId));
                }
                q.or(inner -> inner.isNull(AiDataPackage::getIdOrg).isNull(AiDataPackage::getIdRegion));
                return;
            }
            if (hasRegionId) {
                q.isNull(AiDataPackage::getIdOrg).eq(AiDataPackage::getIdRegion, regionId)
                    .or(inner -> inner.isNull(AiDataPackage::getIdOrg).isNull(AiDataPackage::getIdRegion));
                return;
            }
            q.isNull(AiDataPackage::getIdOrg).isNull(AiDataPackage::getIdRegion);
        });
    }

    private int score(AiDataPackage dataPackage, String orgId, String regionId) {
        if (StringUtils.hasText(orgId) && orgId.equals(dataPackage.getIdOrg())) {
            return 3;
        }
        if (StringUtils.hasText(regionId) && regionId.equals(dataPackage.getIdRegion())) {
            return 2;
        }
        return 1;
    }
}
