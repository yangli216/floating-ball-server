package com.regionalai.floatingball.server.modules.device.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.device.dto.AiDeviceSaveRequest;
import com.regionalai.floatingball.server.modules.device.dto.AiDeviceView;
import com.regionalai.floatingball.server.modules.device.service.DeviceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/devices")
public class AdminDeviceController {

    private final DeviceService deviceService;

    public AdminDeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AiDeviceView>> list(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword,
                                                        HttpServletRequest request) {
        return ApiResponse.success(deviceService.list(current, size, keyword), RequestIdUtils.resolve(request));
    }

    @PostMapping
    public ApiResponse<AiDeviceView> save(@RequestBody AiDeviceSaveRequest body, HttpServletRequest request) {
        return ApiResponse.success(deviceService.save(body), RequestIdUtils.resolve(request));
    }

    @PutMapping("/{idDevice}")
    public ApiResponse<AiDeviceView> update(@PathVariable String idDevice,
                                            @RequestBody AiDeviceSaveRequest body,
                                            HttpServletRequest request) {
        return ApiResponse.success(deviceService.update(idDevice, body), RequestIdUtils.resolve(request));
    }

    @DeleteMapping("/{idDevice}")
    public ApiResponse<Void> invalidate(@PathVariable String idDevice, HttpServletRequest request) {
        deviceService.invalidate(idDevice);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }
}
