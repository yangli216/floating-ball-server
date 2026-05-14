package com.regionalai.floatingball.server.modules.datapackage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.datapackage.dto.MappingDeltaVO;
import com.regionalai.floatingball.server.modules.datapackage.dto.TemplateDeltaVO;
import com.regionalai.floatingball.server.modules.datapackage.entity.AiDataPackage;
import com.regionalai.floatingball.server.modules.datapackage.mapper.AiDataPackageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DataPackageService {

    private static final Logger log = LoggerFactory.getLogger(DataPackageService.class);

    private static final String ACTIVE_ENABLED = "1";
    private static final String ACTIVE_DISABLED = "0";
    private static final String STATUS_DRAFT = "0";
    private static final String STATUS_PUBLISHED = "1";
    private static final String STATUS_ARCHIVED = "2";
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

    public PageResponse<AiDataPackage> list(long current,
                                           long size,
                                           String keyword,
                                           String sdPackageType,
                                           String sdStatus,
                                           String idRegion,
                                           String idOrg) {
        Page<AiDataPackage> page = new Page<AiDataPackage>(current, size);
        LambdaQueryWrapper<AiDataPackage> wrapper = new LambdaQueryWrapper<AiDataPackage>()
            .eq(AiDataPackage::getFgActive, ACTIVE_ENABLED)
            .orderByDesc(AiDataPackage::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(AiDataPackage::getNaPackage, keyword).or().like(AiDataPackage::getCdPackage, keyword));
        }
        if (StringUtils.hasText(sdPackageType)) {
            wrapper.eq(AiDataPackage::getSdPackageType, sdPackageType);
        }
        if (StringUtils.hasText(sdStatus)) {
            wrapper.eq(AiDataPackage::getSdStatus, sdStatus);
        }
        if (StringUtils.hasText(idRegion)) {
            wrapper.eq(AiDataPackage::getIdRegion, idRegion);
        }
        if (StringUtils.hasText(idOrg)) {
            wrapper.eq(AiDataPackage::getIdOrg, idOrg);
        }
        Page<AiDataPackage> result = aiDataPackageMapper.selectPage(page, wrapper);
        return new PageResponse<AiDataPackage>(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Transactional
    public AiDataPackage save(AiDataPackage dataPackage) {
        validateDataPackage(dataPackage);
        dataPackage.setCdPackage(trimToNull(dataPackage.getCdPackage()));
        dataPackage.setNaPackage(trimToNull(dataPackage.getNaPackage()));
        dataPackage.setSdPackageType(trimToNull(dataPackage.getSdPackageType()));
        dataPackage.setVersionNum(trimToNull(dataPackage.getVersionNum()));
        dataPackage.setContentJson(trimToNull(dataPackage.getContentJson()));
        dataPackage.setIdOrg(trimToNull(dataPackage.getIdOrg()));
        dataPackage.setIdRegion(trimToNull(dataPackage.getIdRegion()));
        dataPackage.setFgActive(ACTIVE_ENABLED);
        dataPackage.setSdStatus(resolveStatus(dataPackage.getSdStatus(), STATUS_DRAFT));
        aiDataPackageMapper.insert(dataPackage);
        if (STATUS_PUBLISHED.equals(dataPackage.getSdStatus())) {
            archiveOtherPublishedPackages(dataPackage.getIdPackage(), dataPackage.getSdPackageType(), dataPackage.getIdOrg(), dataPackage.getIdRegion());
        }
        log.info("data package saved. idPackage={}, type={}, version={}", dataPackage.getIdPackage(), dataPackage.getSdPackageType(), dataPackage.getVersionNum());
        return aiDataPackageMapper.selectById(dataPackage.getIdPackage());
    }

    @Transactional
    public AiDataPackage update(String idPackage, AiDataPackage dataPackage) {
        AiDataPackage existing = requireActivePackage(idPackage);
        validateDataPackage(dataPackage);
        existing.setCdPackage(trimToNull(dataPackage.getCdPackage()));
        existing.setNaPackage(trimToNull(dataPackage.getNaPackage()));
        existing.setSdPackageType(trimToNull(dataPackage.getSdPackageType()));
        existing.setVersionNum(trimToNull(dataPackage.getVersionNum()));
        existing.setContentJson(trimToNull(dataPackage.getContentJson()));
        existing.setSdStatus(resolveStatus(dataPackage.getSdStatus(), existing.getSdStatus()));
        existing.setIdOrg(trimToNull(dataPackage.getIdOrg()));
        existing.setIdRegion(trimToNull(dataPackage.getIdRegion()));
        aiDataPackageMapper.updateById(existing);
        if (STATUS_PUBLISHED.equals(existing.getSdStatus())) {
            archiveOtherPublishedPackages(existing.getIdPackage(), existing.getSdPackageType(), existing.getIdOrg(), existing.getIdRegion());
        }
        log.info("data package updated. idPackage={}, type={}, version={}", idPackage, existing.getSdPackageType(), existing.getVersionNum());
        return aiDataPackageMapper.selectById(idPackage);
    }

    @Transactional
    public void publish(String idPackage) {
        AiDataPackage dataPackage = requireActivePackage(idPackage);
        validateDataPackage(dataPackage);
        dataPackage.setSdStatus(STATUS_PUBLISHED);
        aiDataPackageMapper.updateById(dataPackage);
        archiveOtherPublishedPackages(dataPackage.getIdPackage(), dataPackage.getSdPackageType(), dataPackage.getIdOrg(), dataPackage.getIdRegion());
        log.info("data package published. idPackage={}, type={}", idPackage, dataPackage.getSdPackageType());
    }

    public void archive(String idPackage) {
        AiDataPackage dataPackage = requireActivePackage(idPackage);
        dataPackage.setSdStatus(STATUS_ARCHIVED);
        aiDataPackageMapper.updateById(dataPackage);
        log.info("data package archived. idPackage={}", idPackage);
    }

    public void invalidate(String idPackage) {
        AiDataPackage dataPackage = requireActivePackage(idPackage);
        dataPackage.setFgActive(ACTIVE_DISABLED);
        aiDataPackageMapper.updateById(dataPackage);
        log.info("data package invalidated. idPackage={}", idPackage);
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

    public TemplateDeltaVO getBuiltinTemplateSnapshot() {
        return builtinTemplateSeedService.getSnapshotDelta();
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

    private AiDataPackage requireActivePackage(String idPackage) {
        AiDataPackage dataPackage = aiDataPackageMapper.selectById(idPackage);
        if (dataPackage == null || !ACTIVE_ENABLED.equals(dataPackage.getFgActive())) {
            throw new BusinessException("数据包不存在");
        }
        return dataPackage;
    }

    private void validateDataPackage(AiDataPackage dataPackage) {
        if (dataPackage == null) {
            throw new BusinessException("请求体不能为空");
        }
        if (!StringUtils.hasText(dataPackage.getNaPackage())) {
            throw new BusinessException("数据包名称不能为空");
        }
        if (!StringUtils.hasText(dataPackage.getSdPackageType())) {
            throw new BusinessException("数据包类型不能为空");
        }
        if (!isSupportedPackageType(dataPackage.getSdPackageType())) {
            throw new BusinessException("仅支持 template 或 mapping 类型的数据包");
        }
        if (!StringUtils.hasText(dataPackage.getVersionNum())) {
            throw new BusinessException("数据包版本号不能为空");
        }
        if (StringUtils.hasText(dataPackage.getSdStatus()) && !isSupportedStatus(dataPackage.getSdStatus())) {
            throw new BusinessException("数据包状态不合法");
        }
        validateContentJson(dataPackage.getSdPackageType(), dataPackage.getContentJson());
    }

    private void validateContentJson(String packageType, String contentJson) {
        if (!StringUtils.hasText(contentJson)) {
            throw new BusinessException("数据包内容不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(contentJson);
            if (root == null || !root.isObject()) {
                throw new BusinessException("数据包内容必须是 JSON 对象");
            }
            if (PACKAGE_TYPE_TEMPLATE.equals(packageType)) {
                validateTemplateContent(root);
                return;
            }
            validateMappingContent(root);
        } catch (IOException ex) {
            throw new BusinessException("数据包内容必须是合法 JSON");
        }
    }

    private void validateTemplateContent(JsonNode root) {
        JsonNode western = root.get("western");
        JsonNode tcm = root.get("tcm");
        if (western != null && !western.isNull() && !western.isArray()) {
            throw new BusinessException("template 类型的 western 字段必须是数组");
        }
        if (tcm != null && !tcm.isNull() && !tcm.isArray()) {
            throw new BusinessException("template 类型的 tcm 字段必须是数组");
        }
        if (western == null && tcm == null) {
            throw new BusinessException("template 类型至少需要包含 western 或 tcm 字段");
        }
    }

    private void validateMappingContent(JsonNode root) {
        List<String> supportedKeys = Arrays.asList("diagnoses", "medicines", "items", "tcmDiagnoses", "tcmSyndromes", "tcmTreatments");
        boolean hasSupportedField = false;
        for (String key : supportedKeys) {
            JsonNode node = root.get(key);
            if (node == null || node.isNull()) {
                continue;
            }
            hasSupportedField = true;
            if (!node.isTextual()) {
                throw new BusinessException("mapping 类型的 " + key + " 字段必须是字符串");
            }
        }
        if (!hasSupportedField) {
            throw new BusinessException("mapping 类型至少需要填写一个 CSV 字段");
        }
    }

    private void archiveOtherPublishedPackages(String currentId, String packageType, String idOrg, String idRegion) {
        LambdaQueryWrapper<AiDataPackage> wrapper = new LambdaQueryWrapper<AiDataPackage>()
            .eq(AiDataPackage::getFgActive, ACTIVE_ENABLED)
            .eq(AiDataPackage::getSdPackageType, packageType)
            .eq(AiDataPackage::getSdStatus, STATUS_PUBLISHED);
        appendExactScopeCondition(wrapper, idOrg, idRegion);
        List<AiDataPackage> publishedPackages = aiDataPackageMapper.selectList(wrapper);
        for (AiDataPackage item : publishedPackages) {
            if (!item.getIdPackage().equals(currentId)) {
                item.setSdStatus(STATUS_ARCHIVED);
                aiDataPackageMapper.updateById(item);
            }
        }
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

    private void appendExactScopeCondition(LambdaQueryWrapper<AiDataPackage> wrapper, String idOrg, String idRegion) {
        if (StringUtils.hasText(idOrg)) {
            wrapper.eq(AiDataPackage::getIdOrg, idOrg);
        } else {
            wrapper.isNull(AiDataPackage::getIdOrg);
        }
        if (StringUtils.hasText(idRegion)) {
            wrapper.eq(AiDataPackage::getIdRegion, idRegion);
        } else {
            wrapper.isNull(AiDataPackage::getIdRegion);
        }
    }

    private boolean isSupportedPackageType(String packageType) {
        return PACKAGE_TYPE_TEMPLATE.equals(packageType) || PACKAGE_TYPE_MAPPING.equals(packageType);
    }

    private boolean isSupportedStatus(String status) {
        return STATUS_DRAFT.equals(status) || STATUS_PUBLISHED.equals(status) || STATUS_ARCHIVED.equals(status);
    }

    private String resolveStatus(String requestedStatus, String defaultStatus) {
        if (StringUtils.hasText(requestedStatus)) {
            return requestedStatus;
        }
        return StringUtils.hasText(defaultStatus) ? defaultStatus : STATUS_DRAFT;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
