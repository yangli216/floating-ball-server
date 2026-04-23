package com.regionalai.floatingball.server.modules.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.common.util.AesUtils;
import com.regionalai.floatingball.server.common.util.PasswordUtils;
import com.regionalai.floatingball.server.modules.auth.dto.AdminCurrentUser;
import com.regionalai.floatingball.server.modules.auth.dto.AdminLoginRequest;
import com.regionalai.floatingball.server.modules.auth.dto.AdminLoginResponse;
import com.regionalai.floatingball.server.modules.auth.dto.AdminPasswordChangeRequest;
import com.regionalai.floatingball.server.modules.role.entity.AiRole;
import com.regionalai.floatingball.server.modules.role.mapper.AiRoleMapper;
import com.regionalai.floatingball.server.modules.user.entity.AiUser;
import com.regionalai.floatingball.server.modules.user.entity.AiUserRole;
import com.regionalai.floatingball.server.modules.user.mapper.AiUserMapper;
import com.regionalai.floatingball.server.modules.user.mapper.AiUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AiUserMapper aiUserMapper;

    @Mock
    private AiUserRoleMapper aiUserRoleMapper;

    @Mock
    private AiRoleMapper aiRoleMapper;

    private AdminAuthService adminAuthService;
    private AdminTokenService adminTokenService;

    @BeforeEach
    void setUp() {
        adminTokenService = new AdminTokenService(new AesUtils("1234567890abcdef"), new ObjectMapper());
        adminAuthService = new AdminAuthService(aiUserMapper, aiUserRoleMapper, aiRoleMapper, adminTokenService);
    }

    @Test
    void loginShouldReturnEncryptedTokenAndCurrentUser() {
        AiUser user = new AiUser();
        user.setIdUser("USER001");
        user.setCdUser("admin");
        user.setNaUser("系统管理员");
        user.setIdOrg("ORG001");
        user.setSdStatus("1");
        user.setFgActive("1");
        user.setPasswordHash(PasswordUtils.sha256("admin123"));

        AiUserRole userRole = new AiUserRole();
        userRole.setIdUser("USER001");
        userRole.setIdRole("ROLE001");
        userRole.setFgActive("1");

        AiRole role = new AiRole();
        role.setIdRole("ROLE001");
        role.setCdRole("SYSTEM_ADMIN");
        role.setNaRole("系统管理员");
        role.setSdStatus("1");
        role.setFgActive("1");

        when(aiUserMapper.selectOne(any())).thenReturn(user);
        when(aiUserRoleMapper.selectList(any())).thenReturn(Collections.singletonList(userRole));
        when(aiRoleMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(role));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        AdminLoginResponse response = adminAuthService.login(request);

        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
        assertEquals("admin", response.getUser().getCdUser());
        assertEquals(Collections.singletonList("SYSTEM_ADMIN"), response.getUser().getRoles());

        AdminCurrentUser parsed = adminTokenService.parse(response.getToken());
        assertEquals("USER001", parsed.getIdUser());
        assertEquals("ORG001", parsed.getIdOrg());
    }

    @Test
    void loginShouldRejectWrongPassword() {
        AiUser user = new AiUser();
        user.setCdUser("admin");
        user.setSdStatus("1");
        user.setFgActive("1");
        user.setPasswordHash(PasswordUtils.sha256("admin123"));

        when(aiUserMapper.selectOne(any())).thenReturn(user);

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        BusinessException ex = assertThrows(BusinessException.class, () -> adminAuthService.login(request));

        assertEquals("账号或密码错误", ex.getMessage());
    }

    @Test
    void changePasswordShouldUpdatePasswordHashForCurrentUser() {
        AiUser user = new AiUser();
        user.setIdUser("USER001");
        user.setCdUser("admin");
        user.setSdStatus("1");
        user.setFgActive("1");
        user.setPasswordHash(PasswordUtils.sha256("admin123"));

        when(aiUserMapper.selectById("USER001")).thenReturn(user);

        AdminCurrentUser currentUser = new AdminCurrentUser();
        currentUser.setIdUser("USER001");
        currentUser.setCdUser("admin");

        AdminPasswordChangeRequest request = new AdminPasswordChangeRequest();
        request.setOldPassword("admin123");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        adminAuthService.changePassword(currentUser, request);

        verify(aiUserMapper).updateById(argThat(item ->
            "USER001".equals(item.getIdUser())
                && PasswordUtils.matches("newPass123", item.getPasswordHash())
        ));
    }

    @Test
    void changePasswordShouldRejectWrongOldPassword() {
        AiUser user = new AiUser();
        user.setIdUser("USER001");
        user.setSdStatus("1");
        user.setFgActive("1");
        user.setPasswordHash(PasswordUtils.sha256("admin123"));

        when(aiUserMapper.selectById("USER001")).thenReturn(user);

        AdminCurrentUser currentUser = new AdminCurrentUser();
        currentUser.setIdUser("USER001");

        AdminPasswordChangeRequest request = new AdminPasswordChangeRequest();
        request.setOldPassword("wrong-pass");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        BusinessException ex = assertThrows(BusinessException.class, () -> adminAuthService.changePassword(currentUser, request));

        assertEquals("当前密码错误", ex.getMessage());
    }

    @Test
    void bootstrapResetPasswordShouldUpdateTargetUser() {
        AiUser user = new AiUser();
        user.setIdUser("USER001");
        user.setCdUser("admin");
        user.setSdStatus("1");
        user.setFgActive("1");
        user.setPasswordHash(PasswordUtils.sha256("admin123"));

        when(aiUserMapper.selectOne(any())).thenReturn(user);

        adminAuthService.bootstrapResetPassword("admin", "recover123");

        verify(aiUserMapper).updateById(argThat(item ->
            "USER001".equals(item.getIdUser())
                && PasswordUtils.matches("recover123", item.getPasswordHash())
        ));
    }
}
