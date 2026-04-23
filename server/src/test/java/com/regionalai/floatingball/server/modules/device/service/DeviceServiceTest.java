package com.regionalai.floatingball.server.modules.device.service;

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
import com.regionalai.floatingball.server.modules.region.mapper.AiRegionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService(aiDeviceMapper, aiOrgMapper, aiRegionMapper);
    }

    @Test
    void registerShouldRefreshExistingDeviceAndGenerateTokenWhenMissing() {
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

        RegisterDeviceResponse response = deviceService.register(request);

        assertEquals("DEV001", response.getIdDevice());
        assertEquals(30, response.getHeartbeatInterval().intValue());
        assertFalse(response.getDeviceToken().isEmpty());
        assertEquals(32, response.getDeviceToken().length());
        assertEquals("诊室设备", existing.getNaDevice());
        assertEquals("1.0.0", existing.getClientVersion());
        assertEquals("Windows 11", existing.getOsInfo());
        verify(aiDeviceMapper, times(1)).updateById(existing);
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

    private AiOrg buildOrg(String idOrg, String idRegion) {
        AiOrg org = new AiOrg();
        org.setIdOrg(idOrg);
        org.setIdRegion(idRegion);
        org.setFgActive("1");
        return org;
    }
}
