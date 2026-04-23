package com.regionalai.floatingball.server.modules.role.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.role.dto.AdminRoleSaveRequest;
import com.regionalai.floatingball.server.modules.role.entity.AiRole;
import com.regionalai.floatingball.server.modules.role.service.RoleService;
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
@RequestMapping("/admin/api/roles")
public class AdminRoleController {

    private final RoleService roleService;

    public AdminRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AiRole>> list(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword,
                                                  HttpServletRequest request) {
        return ApiResponse.success(roleService.list(current, size, keyword), RequestIdUtils.resolve(request));
    }

    @PostMapping
    public ApiResponse<AiRole> save(@RequestBody AdminRoleSaveRequest request,
                                    HttpServletRequest httpServletRequest) {
        return ApiResponse.success(roleService.save(request), RequestIdUtils.resolve(httpServletRequest));
    }

    @PutMapping("/{idRole}")
    public ApiResponse<AiRole> update(@PathVariable String idRole,
                                      @RequestBody AdminRoleSaveRequest request,
                                      HttpServletRequest httpServletRequest) {
        return ApiResponse.success(roleService.update(idRole, request), RequestIdUtils.resolve(httpServletRequest));
    }

    @DeleteMapping("/{idRole}")
    public ApiResponse<Void> invalidate(@PathVariable String idRole, HttpServletRequest request) {
        roleService.invalidate(idRole);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }
}
