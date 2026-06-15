package com.regionalai.floatingball.server.modules.device.service;

import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.common.exception.UpdateRequiredException;
import com.regionalai.floatingball.server.common.db.DatabaseDialect;
import com.regionalai.floatingball.server.common.util.MaskingUtils;
import com.regionalai.floatingball.server.modules.device.dto.AiDeviceSaveRequest;
import com.regionalai.floatingball.server.modules.device.dto.RegisterDeviceRequest;
import com.regionalai.floatingball.server.modules.device.dto.RegisterDeviceResponse;
import com.regionalai.floatingball.server.modules.device.dto.AiDeviceView;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.device.mapper.AiDeviceMapper;
import com.regionalai.floatingball.server.modules.org.entity.AiOrg;
import com.regionalai.floatingball.server.modules.org.mapper.AiOrgMapper;
import com.regionalai.floatingball.server.modules.region.mapper.AiRegionMapper;
import com.regionalai.floatingball.server.modules.release.dto.ReleasePolicyView;
import com.regionalai.floatingball.server.modules.release.service.ReleaseService;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private AiDeviceMapper aiDeviceMapper;

    @Mock
    private AiOrgMapper aiOrgMapper;

    @Mock
    private AiRegionMapper aiRegionMapper;

    @Mock
    private ReleaseService releaseService;

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService(aiDeviceMapper, aiOrgMapper, aiRegionMapper, releaseService, oracleDialect());
    }

    private DatabaseDialect oracleDialect() {
        return new DatabaseDialect(DatabaseDialect.Kind.ORACLE);
    }

    @Test
    void registerShouldFillPublicKeyForLegacyDeviceWithoutPublicKey() {
        AiOrg org = buildOrg("ORG001", "REG001");
        AiDevice existing = new AiDevice();
        existing.setIdDevice("DEV001");
        existing.setFgActive("1");
        existing.setDeviceToken(null);

        when(aiOrgMapper.selectOne(any())).thenReturn(org);
        when(aiDeviceMapper.selectOne(any())).thenReturn(existing);

        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setCdOrg("ORG-CODE");
        request.setCdDevice("DEV-CODE");
        request.setNaDevice("诊室设备");
        request.setClientVersion("1.0.0");
        request.setOsInfo("Windows 11");
        request.setPublicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtest-public-key-base64");

        RegisterDeviceResponse response = deviceService.register(request, "10.10.1.8");

        assertEquals("DEV001", response.getIdDevice());
        assertEquals(30, response.getHeartbeatInterval().intValue());
        assertFalse(response.getDeviceToken().isEmpty());
        assertEquals(32, response.getDeviceToken().length());
        assertTrue(response.getHasPublicKey());
        assertEquals("诊室设备", existing.getNaDevice());
        assertEquals("1.0.0", existing.getClientVersion());
        assertEquals("Windows 11", existing.getOsInfo());
        assertEquals("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtest-public-key-base64", existing.getDevicePublicKey());
        assertEquals("10.10.1.8", existing.getRegisterIp());
        assertEquals("10.10.1.8", existing.getLastSeenIp());
        verify(aiDeviceMapper, times(1)).updateById(existing);
    }

    @Test
    void registerShouldRejectAnonymousTakeoverWhenExistingDeviceHasPublicKey() {
        AiOrg org = buildOrg("ORG001", "REG001");
        AiDevice existing = new AiDevice();
        existing.setIdDevice("DEV001");
        existing.setFgActive("1");
        existing.setDeviceToken("existing-token");
        existing.setDevicePublicKey("existing-public-key");

        when(aiOrgMapper.selectOne(any())).thenReturn(org);
        when(aiDeviceMapper.selectOne(any())).thenReturn(existing);

        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setCdOrg("ORG-CODE");
        request.setCdDevice("DEV-CODE");
        request.setNaDevice("攻击者设备");
        request.setClientVersion("1.0.1");
        request.setOsInfo("Windows 11");
        request.setPublicKey("attacker-public-key");

        BusinessException ex = assertThrows(BusinessException.class, () -> deviceService.register(request));

        assertEquals("设备已注册，请使用现有设备令牌；如本机密钥丢失请重新生成设备编码后注册", ex.getMessage());
        assertEquals("existing-token", existing.getDeviceToken());
        assertEquals("existing-public-key", existing.getDevicePublicKey());
        verify(aiDeviceMapper, never()).updateById(any(AiDevice.class));
        verify(aiDeviceMapper, never()).insert(any(AiDevice.class));
    }

    @Test
    void registerShouldRejectDisabledDeviceCode() {
        AiOrg org = buildOrg("ORG001", "REG001");
        AiDevice disabled = new AiDevice();
        disabled.setIdDevice("DEV-DISABLED");
        disabled.setCdDevice("DEV-CODE");
        disabled.setFgActive("0");
        disabled.setDeviceToken("disabled-token");
        disabled.setDevicePublicKey("old-public-key");

        when(aiOrgMapper.selectOne(any())).thenReturn(org);
        when(aiDeviceMapper.selectOne(any())).thenReturn(null, disabled);

        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setCdOrg("ORG-CODE");
        request.setCdDevice("DEV-CODE");
        request.setNaDevice("旧版本终端");
        request.setClientVersion("1.0.0");
        request.setOsInfo("Windows 11");
        request.setPublicKey("new-public-key");

        BusinessException ex = assertThrows(BusinessException.class, () -> deviceService.register(request));

        assertEquals("DEVICE-DISABLED", ex.getCode());
        assertEquals("设备已被管理员停用，请联系管理员重新发放令牌后再连接", ex.getMessage());
        verify(aiDeviceMapper, never()).insert(any(AiDevice.class));
        verify(aiDeviceMapper, never()).updateById(any(AiDevice.class));
    }

    @Test
    void registerShouldCreateNewDeviceWhenResetDeleteRemovedPreviousRecord() {
        AiOrg org = buildOrg("ORG001", "REG001");
        when(aiOrgMapper.selectOne(any())).thenReturn(org);
        when(aiDeviceMapper.selectOne(any())).thenReturn(null, null);
        when(aiDeviceMapper.insert(any(AiDevice.class))).thenAnswer(invocation -> {
            AiDevice inserted = invocation.getArgument(0);
            inserted.setIdDevice("DEV-NEW");
            return 1;
        });

        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setCdOrg("ORG-CODE");
        request.setCdDevice("DEV-CODE");
        request.setNaDevice("重置后终端");
        request.setClientVersion("1.2.13");
        request.setOsInfo("Windows 11");
        request.setPublicKey("new-public-key");

        RegisterDeviceResponse response = deviceService.register(request, "172.16.0.10");

        ArgumentCaptor<AiDevice> captor = ArgumentCaptor.forClass(AiDevice.class);
        verify(aiDeviceMapper).insert(captor.capture());
        AiDevice saved = captor.getValue();
        assertEquals("DEV-CODE", saved.getCdDevice());
        assertEquals("ORG001", saved.getIdOrg());
        assertEquals("REG001", saved.getIdRegion());
        assertEquals("0", saved.getSdStatus());
        assertEquals("1", saved.getFgActive());
        assertEquals("new-public-key", saved.getDevicePublicKey());
        assertEquals("172.16.0.10", saved.getRegisterIp());
        assertEquals("172.16.0.10", saved.getLastSeenIp());
        assertEquals("DEV-NEW", response.getIdDevice());
        assertEquals(saved.getDeviceToken(), response.getDeviceToken());
        assertTrue(response.getHasPublicKey());
        verify(aiDeviceMapper, never()).updateById(any(AiDevice.class));
    }

    @Test
    void registerShouldAllowAdminIssuedActivePlaceholderWhenDisabledHistoryExists() {
        AiOrg org = buildOrg("ORG001", "REG001");
        AiDevice activePlaceholder = new AiDevice();
        activePlaceholder.setIdDevice("DEV-NEW");
        activePlaceholder.setCdDevice("DEV-CODE");
        activePlaceholder.setFgActive("1");
        activePlaceholder.setDeviceToken("active-token");

        when(aiOrgMapper.selectOne(any())).thenReturn(org);
        when(aiDeviceMapper.selectOne(any())).thenReturn(activePlaceholder);

        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setCdOrg("ORG-CODE");
        request.setCdDevice("DEV-CODE");
        request.setNaDevice("恢复终端");
        request.setClientVersion("1.2.13");
        request.setOsInfo("Windows 11");
        request.setPublicKey("restored-public-key");

        RegisterDeviceResponse response = deviceService.register(request, "172.16.0.9");

        assertEquals("DEV-NEW", response.getIdDevice());
        assertEquals("active-token", response.getDeviceToken());
        assertTrue(response.getHasPublicKey());
        assertEquals("restored-public-key", activePlaceholder.getDevicePublicKey());
        assertEquals("172.16.0.9", activePlaceholder.getRegisterIp());
        assertEquals("172.16.0.9", activePlaceholder.getLastSeenIp());
        verify(aiDeviceMapper).updateById(activePlaceholder);
        verify(aiDeviceMapper, never()).insert(any(AiDevice.class));
    }

    @Test
    void registerShouldRejectWhenClientVersionRequiresUpdate() {
        AiOrg org = buildOrg("ORG001", "REG001");
        ReleasePolicyView policy = new ReleasePolicyView();
        policy.setMinSupportedVersion("1.2.13");

        when(aiOrgMapper.selectOne(any())).thenReturn(org);
        when(releaseService.isUpdateRequired(eq("production"), eq("1.2.12"))).thenReturn(true);
        when(releaseService.getRequiredPolicy("production")).thenReturn(policy);

        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setCdOrg("ORG-CODE");
        request.setCdDevice("DEV-CODE");
        request.setClientVersion("1.2.12");
        request.setUpdateChannel("production");
        request.setPublicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtest-public-key");

        UpdateRequiredException ex = assertThrows(UpdateRequiredException.class, () -> deviceService.register(request));

        assertEquals("1.2.13", ex.getMinSupportedVersion());
        verify(aiDeviceMapper, never()).insert(any(AiDevice.class));
        verify(aiDeviceMapper, never()).updateById(any(AiDevice.class));
    }

    @Test
    void registerShouldTranslateDuplicateOrgCodeToBusinessError() {
        when(aiOrgMapper.selectOne(any())).thenThrow(new TooManyResultsException("found: 2"));

        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setCdOrg("ORG-CODE");
        request.setCdDevice("DEV-CODE");
        request.setClientVersion("1.2.13");
        request.setPublicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtest-public-key");

        BusinessException ex = assertThrows(BusinessException.class, () -> deviceService.register(request));

        assertEquals("机构编码重复，请先在管理端清理重复机构: ORG-CODE", ex.getMessage());
        verify(aiDeviceMapper, never()).insert(any(AiDevice.class));
        verify(aiDeviceMapper, never()).updateById(any(AiDevice.class));
    }

    @Test
    void saveShouldPopulateOrgRegionAndMaskGeneratedToken() {
        AiOrg org = buildOrg("ORG001", "REG001");
        when(aiOrgMapper.selectOne(any())).thenReturn(org);
        when(aiDeviceMapper.selectOne(any())).thenReturn(null);

        AiDeviceSaveRequest request = new AiDeviceSaveRequest();
        request.setCdDevice(" DEV-NEW ");
        request.setNaDevice("新终端");
        request.setIdOrg("ORG001");
        request.setClientVersion("2.0.0");
        request.setOsInfo("Windows 10");

        AiDeviceView view = deviceService.save(request);

        ArgumentCaptor<AiDevice> captor = ArgumentCaptor.forClass(AiDevice.class);
        verify(aiDeviceMapper).insert(captor.capture());
        AiDevice saved = captor.getValue();
        assertEquals("DEV-NEW", saved.getCdDevice());
        assertEquals("ORG001", saved.getIdOrg());
        assertEquals("REG001", saved.getIdRegion());
        assertEquals("0", saved.getSdStatus());
        assertEquals("1", saved.getFgActive());
        assertEquals(32, saved.getDeviceToken().length());
        assertEquals(MaskingUtils.maskSecret(saved.getDeviceToken()), view.getDeviceTokenMasked());
        assertEquals("ORG001", view.getIdOrg());
        assertEquals("REG001", view.getIdRegion());
    }

    @Test
    void saveShouldRejectDuplicateDeviceCodeWithinSameOrg() {
        AiOrg org = buildOrg("ORG001", "REG001");
        when(aiOrgMapper.selectOne(any())).thenReturn(org);
        when(aiDeviceMapper.selectOne(any())).thenReturn(new AiDevice());

        AiDeviceSaveRequest request = new AiDeviceSaveRequest();
        request.setCdDevice("DEV-EXISTS");
        request.setNaDevice("重复终端");
        request.setIdOrg("ORG001");

        BusinessException ex = assertThrows(BusinessException.class, () -> deviceService.save(request));

        assertEquals("设备编码已存在", ex.getMessage());
        verify(aiDeviceMapper, never()).insert(any(AiDevice.class));
    }

    @Test
    void saveShouldTranslateDatabaseUniqueConflictToBusinessError() {
        AiOrg org = buildOrg("ORG001", "REG001");
        when(aiOrgMapper.selectOne(any())).thenReturn(org);
        when(aiDeviceMapper.selectOne(any())).thenReturn(null);
        when(aiDeviceMapper.insert(any(AiDevice.class))).thenThrow(new DuplicateKeyException("uk_c_ai_device_code_org_active"));

        AiDeviceSaveRequest request = new AiDeviceSaveRequest();
        request.setCdDevice("DEV-NEW");
        request.setNaDevice("新终端");
        request.setIdOrg("ORG001");

        BusinessException ex = assertThrows(BusinessException.class, () -> deviceService.save(request));

        assertEquals("设备编码已存在", ex.getMessage());
    }

    @Test
    void heartbeatShouldMarkDeviceOnlineAndPersistLastHeartbeat() {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setSdStatus("0");

        deviceService.heartbeat(device, "192.168.1.23");

        assertEquals("1", device.getSdStatus());
        assertEquals("192.168.1.23", device.getLastSeenIp());
        assertTrue(device.getDtLastHeartbeat() != null);
        verify(aiDeviceMapper).updateById(device);
    }

    @Test
    void heartbeatShouldKeepLastSeenIpWhenClientIpUnknown() {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setSdStatus("0");
        device.setLastSeenIp("192.168.1.23");

        deviceService.heartbeat(device, "unknown");

        assertEquals("1", device.getSdStatus());
        assertEquals("192.168.1.23", device.getLastSeenIp());
        assertTrue(device.getDtLastHeartbeat() != null);
        verify(aiDeviceMapper).updateById(device);
    }

    @Test
    void updateShouldKeepExistingStatusWhenRequestStatusBlank() {
        AiDevice existing = new AiDevice();
        existing.setIdDevice("DEV001");
        existing.setCdDevice("OLD-CODE");
        existing.setIdOrg("ORG001");
        existing.setFgActive("1");
        existing.setSdStatus("1");
        existing.setDeviceToken("1234567890abcdef1234567890abcdef");

        AiOrg targetOrg = buildOrg("ORG002", "REG002");
        when(aiDeviceMapper.selectById("DEV001")).thenReturn(existing);
        when(aiOrgMapper.selectOne(any())).thenReturn(targetOrg);
        when(aiDeviceMapper.selectOne(any())).thenReturn(null);

        AiDeviceSaveRequest request = new AiDeviceSaveRequest();
        request.setCdDevice(" NEW-CODE ");
        request.setNaDevice("改名终端");
        request.setIdOrg("ORG002");
        request.setSdStatus("");
        request.setClientVersion("2.0.0");
        request.setOsInfo("macOS");

        AiDeviceView view = deviceService.update("DEV001", request);

        assertEquals("NEW-CODE", existing.getCdDevice());
        assertEquals("REG002", existing.getIdRegion());
        assertEquals("1", existing.getSdStatus());
        assertEquals("ORG002", view.getIdOrg());
        assertEquals("REG002", view.getIdRegion());
        verify(aiDeviceMapper).updateById(existing);
    }

    @Test
    void invalidateShouldSoftDeleteAndTakeDeviceOffline() {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setFgActive("1");
        device.setSdStatus("1");
        when(aiDeviceMapper.selectById("DEV001")).thenReturn(device);

        deviceService.invalidate("DEV001");

        assertEquals("0", device.getFgActive());
        assertEquals("0", device.getSdStatus());
        verify(aiDeviceMapper).updateById(device);
    }

    @Test
    void deleteForResetShouldPhysicallyRemoveDeviceRecord() {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setFgActive("1");
        device.setSdStatus("1");
        when(aiDeviceMapper.selectById("DEV001")).thenReturn(device);

        deviceService.deleteForReset("DEV001");

        verify(aiDeviceMapper).deleteById("DEV001");
        verify(aiDeviceMapper, never()).updateById(any(AiDevice.class));
    }

    @Test
    void deleteForResetShouldRejectMissingDevice() {
        when(aiDeviceMapper.selectById("MISSING")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> deviceService.deleteForReset("MISSING"));

        assertEquals("设备不存在", ex.getMessage());
        verify(aiDeviceMapper, never()).deleteById(any(String.class));
    }

    private AiOrg buildOrg(String idOrg, String idRegion) {
        AiOrg org = new AiOrg();
        org.setIdOrg(idOrg);
        org.setIdRegion(idRegion);
        org.setFgActive("1");
        return org;
    }
}
