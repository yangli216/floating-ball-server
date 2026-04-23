package com.regionalai.floatingball.server.modules.role.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.GlobalExceptionHandler;
import com.regionalai.floatingball.server.modules.role.dto.AdminRoleSaveRequest;
import com.regionalai.floatingball.server.modules.role.entity.AiRole;
import com.regionalai.floatingball.server.modules.role.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminRoleControllerTest {

    @Mock
    private RoleService roleService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRoleController(roleService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void listShouldReturnPagedRoles() throws Exception {
        AiRole role = new AiRole();
        role.setIdRole("ROLE001");
        role.setCdRole("SYSTEM_ADMIN");
        role.setNaRole("系统管理员");
        role.setDesRole("拥有全部后台权限");
        role.setSdStatus("1");

        PageResponse<AiRole> pageResponse = new PageResponse<AiRole>(1, 10, 1, Collections.singletonList(role));
        when(roleService.list(1, 10, "SYSTEM")).thenReturn(pageResponse);

        mockMvc.perform(get("/admin/api/roles")
                .param("current", "1")
                .param("size", "10")
                .param("keyword", "SYSTEM")
                .header("X-Request-Id", "RID-role-list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.requestId").value("RID-role-list"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].cdRole").value("SYSTEM_ADMIN"))
            .andExpect(jsonPath("$.data.records[0].naRole").value("系统管理员"));
    }

    @Test
    void saveShouldReturnCreatedRole() throws Exception {
        AiRole role = new AiRole();
        role.setIdRole("ROLE001");
        role.setCdRole("SYSTEM_ADMIN");
        role.setNaRole("系统管理员");
        role.setDesRole("拥有全部后台权限");
        role.setSdStatus("1");

        AdminRoleSaveRequest request = new AdminRoleSaveRequest();
        request.setCdRole("SYSTEM_ADMIN");
        request.setNaRole("系统管理员");
        request.setDesRole("拥有全部后台权限");
        request.setSdStatus("1");

        when(roleService.save(any(AdminRoleSaveRequest.class))).thenReturn(role);

        mockMvc.perform(post("/admin/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-Id", "RID-role-save")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.requestId").value("RID-role-save"))
            .andExpect(jsonPath("$.data.idRole").value("ROLE001"))
            .andExpect(jsonPath("$.data.cdRole").value("SYSTEM_ADMIN"));
    }

    @Test
    void invalidateShouldDelegateToService() throws Exception {
        mockMvc.perform(delete("/admin/api/roles/ROLE001")
                .header("X-Request-Id", "RID-role-delete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.requestId").value("RID-role-delete"));

        verify(roleService).invalidate(eq("ROLE001"));
    }
}
