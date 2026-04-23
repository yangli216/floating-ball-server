package com.regionalai.floatingball.server.modules.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.common.util.MaskingUtils;
import com.regionalai.floatingball.server.modules.device.dto.AiDeviceSaveRequest;
import com.regionalai.floatingball.server.modules.device.dto.RegisterDeviceRequest;
import com.regionalai.floatingball.server.modules.device.dto.RegisterDeviceResponse;
import com.regionalai.floatingball.server.modules.device.dto.AiDeviceView;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.device.mapper.AiDeviceMapper;
import com.regionalai.floatingball.server.modules.org.entity.AiOrg;
import com.regionalai.floatingball.server.modules.org.mapper.AiOrgMapper;
import com.regionalai.floatingball.server.modules.region.entity.AiRegion;
import com.regionalai.floatingball.server.modules.region.mapper.AiRegionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private static final int DEFAULT_HEARTBEAT_INTERVAL = 30;

    private final AiDeviceMapper aiDeviceMapper;
    private final AiOrgMapper aiOrgMapper;
    private final AiRegionMapper aiRegionMapper;

    public DeviceService(AiDeviceMapper aiDeviceMapper, AiOrgMapper aiOrgMapper, AiRegionMapper aiRegionMapper) {
        this.aiDeviceMapper = aiDeviceMapper;
        this.aiOrgMapper = aiOrgMapper;
        this.aiRegionMapper = aiRegionMapper;
    }

    public RegisterDeviceResponse register(RegisterDeviceRequest request) {
        AiOrg org = aiOrgMapper.selectOne(new LambdaQueryWrapper<AiOrg>()
            .eq(AiOrg::getCdOrg, request.getCdOrg())
            .eq(AiOrg::getFgActive, "1"));
        if (org == null) {
            throw new BusinessException("机构编码不存在: " + request.getCdOrg());
        }

        AiDevice existing = aiDeviceMapper.selectOne(new LambdaQueryWrapper<AiDevice>()
            .eq(AiDevice::getCdDevice, request.getCdDevice())
            .eq(AiDevice::getIdOrg, org.getIdOrg())
            .eq(AiDevice::getFgActive, "1")
            .last("FETCH FIRST 1 ROWS ONLY"));

        if (existing != null) {
            existing.setNaDevice(request.getNaDevice());
            existing.setClientVersion(request.getClientVersion());
            existing.setOsInfo(request.getOsInfo());
            if (!StringUtils.hasText(existing.getDeviceToken())) {
                existing.setDeviceToken(generateToken());
            }
            aiDeviceMapper.updateById(existing);
            return new RegisterDeviceResponse(existing.getIdDevice(), existing.getDeviceToken(), DEFAULT_HEARTBEAT_INTERVAL);
        }

        AiDevice device = new AiDevice();
        device.setCdDevice(request.getCdDevice());
        device.setNaDevice(request.getNaDevice());
        device.setIdOrg(org.getIdOrg());
        device.setIdRegion(org.getIdRegion());
        device.setDeviceToken(generateToken());
        device.setSdStatus("0");
        device.setDtRegistered(LocalDateTime.now());
        device.setClientVersion(request.getClientVersion());
        device.setOsInfo(request.getOsInfo());
        device.setFgActive("1");
        aiDeviceMapper.insert(device);
        return new RegisterDeviceResponse(device.getIdDevice(), device.getDeviceToken(), DEFAULT_HEARTBEAT_INTERVAL);
    }

    public AiDevice findActiveByToken(String token) {
        return aiDeviceMapper.selectOne(new LambdaQueryWrapper<AiDevice>()
            .eq(AiDevice::getDeviceToken, token)
            .eq(AiDevice::getFgActive, "1")
            .last("FETCH FIRST 1 ROWS ONLY"));
    }

    public void heartbeat(AiDevice device) {
        device.setDtLastHeartbeat(LocalDateTime.now());
        device.setSdStatus("1");
        aiDeviceMapper.updateById(device);
    }

    public PageResponse<AiDeviceView> list(long current, long size, String keyword) {
        Page<AiDevice> page = new Page<AiDevice>(current, size);
        LambdaQueryWrapper<AiDevice> wrapper = new LambdaQueryWrapper<AiDevice>()
            .eq(AiDevice::getFgActive, "1")
            .orderByDesc(AiDevice::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(AiDevice::getCdDevice, keyword).or().like(AiDevice::getNaDevice, keyword));
        }
        Page<AiDevice> result = aiDeviceMapper.selectPage(page, wrapper);
        return new PageResponse<AiDeviceView>(result.getCurrent(), result.getSize(), result.getTotal(), toViews(result.getRecords()));
    }

    public AiDeviceView save(AiDeviceSaveRequest request) {
        validateSaveRequest(request);
        AiOrg org = requireActiveOrg(request.getIdOrg());
        ensureUniqueCode(request.getCdDevice(), org.getIdOrg(), null);

        AiDevice device = new AiDevice();
        device.setCdDevice(request.getCdDevice().trim());
        device.setNaDevice(request.getNaDevice());
        device.setIdOrg(org.getIdOrg());
        device.setIdRegion(org.getIdRegion());
        device.setDeviceToken(generateToken());
        device.setSdStatus(StringUtils.hasText(request.getSdStatus()) ? request.getSdStatus() : "0");
        device.setDtRegistered(LocalDateTime.now());
        device.setClientVersion(request.getClientVersion());
        device.setOsInfo(request.getOsInfo());
        device.setFgActive("1");
        aiDeviceMapper.insert(device);
        return toView(device, org, null);
    }

    public AiDeviceView update(String idDevice, AiDeviceSaveRequest request) {
        validateSaveRequest(request);
        AiDevice existing = aiDeviceMapper.selectById(idDevice);
        if (existing == null || !"1".equals(existing.getFgActive())) {
            throw new BusinessException("设备不存在");
        }

        AiOrg org = requireActiveOrg(request.getIdOrg());
        ensureUniqueCode(request.getCdDevice(), org.getIdOrg(), idDevice);

        existing.setCdDevice(request.getCdDevice().trim());
        existing.setNaDevice(request.getNaDevice());
        existing.setIdOrg(org.getIdOrg());
        existing.setIdRegion(org.getIdRegion());
        existing.setSdStatus(StringUtils.hasText(request.getSdStatus()) ? request.getSdStatus() : existing.getSdStatus());
        existing.setClientVersion(request.getClientVersion());
        existing.setOsInfo(request.getOsInfo());
        aiDeviceMapper.updateById(existing);
        return toView(existing, org, null);
    }

    public void invalidate(String idDevice) {
        AiDevice device = aiDeviceMapper.selectById(idDevice);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        device.setFgActive("0");
        device.setSdStatus("0");
        aiDeviceMapper.updateById(device);
    }

    private void validateSaveRequest(AiDeviceSaveRequest request) {
        if (request == null) {
            throw new BusinessException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getCdDevice())) {
            throw new BusinessException("设备编码不能为空");
        }
        if (!StringUtils.hasText(request.getIdOrg())) {
            throw new BusinessException("所属机构不能为空");
        }
    }

    private AiOrg requireActiveOrg(String idOrg) {
        AiOrg org = aiOrgMapper.selectOne(new LambdaQueryWrapper<AiOrg>()
            .eq(AiOrg::getIdOrg, idOrg)
            .eq(AiOrg::getFgActive, "1")
            .last("FETCH FIRST 1 ROWS ONLY"));
        if (org == null) {
            throw new BusinessException("机构不存在");
        }
        return org;
    }

    private void ensureUniqueCode(String cdDevice, String idOrg, String excludeIdDevice) {
        LambdaQueryWrapper<AiDevice> wrapper = new LambdaQueryWrapper<AiDevice>()
            .eq(AiDevice::getCdDevice, cdDevice.trim())
            .eq(AiDevice::getIdOrg, idOrg)
            .eq(AiDevice::getFgActive, "1");
        if (StringUtils.hasText(excludeIdDevice)) {
            wrapper.ne(AiDevice::getIdDevice, excludeIdDevice);
        }
        AiDevice existing = aiDeviceMapper.selectOne(wrapper.last("FETCH FIRST 1 ROWS ONLY"));
        if (existing != null) {
            throw new BusinessException("设备编码已存在");
        }
    }

    private List<AiDeviceView> toViews(List<AiDevice> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, AiOrg> orgMap = aiOrgMapper.selectBatchIds(records.stream()
                .map(AiDevice::getIdOrg)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList()))
            .stream()
            .collect(Collectors.toMap(AiOrg::getIdOrg, Function.identity(), (left, right) -> left));
        Map<String, AiRegion> regionMap = aiRegionMapper.selectBatchIds(records.stream()
                .map(AiDevice::getIdRegion)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList()))
            .stream()
            .collect(Collectors.toMap(AiRegion::getIdRegion, Function.identity(), (left, right) -> left));
        return records.stream()
            .map(item -> toView(item, orgMap.get(item.getIdOrg()), regionMap.get(item.getIdRegion())))
            .collect(Collectors.toList());
    }

    private AiDeviceView toView(AiDevice device, AiOrg org, AiRegion region) {
        AiDeviceView view = new AiDeviceView();
        view.setIdDevice(device.getIdDevice());
        view.setCdDevice(device.getCdDevice());
        view.setNaDevice(device.getNaDevice());
        view.setIdOrg(device.getIdOrg());
        view.setNaOrg(org == null ? null : org.getNaOrg());
        view.setIdRegion(device.getIdRegion());
        view.setNaRegion(region == null ? null : region.getNaRegion());
        view.setDeviceTokenMasked(MaskingUtils.maskSecret(device.getDeviceToken()));
        view.setSdStatus(device.getSdStatus());
        view.setClientVersion(device.getClientVersion());
        view.setOsInfo(device.getOsInfo());
        view.setDtLastHeartbeat(device.getDtLastHeartbeat());
        view.setDtRegistered(device.getDtRegistered());
        return view;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
