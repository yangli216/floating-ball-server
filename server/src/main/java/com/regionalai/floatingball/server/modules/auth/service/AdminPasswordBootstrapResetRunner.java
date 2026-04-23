package com.regionalai.floatingball.server.modules.auth.service;

import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.auth.config.AdminSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminPasswordBootstrapResetRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminPasswordBootstrapResetRunner.class);

    private final AdminSecurityProperties adminSecurityProperties;
    private final AdminAuthService adminAuthService;

    public AdminPasswordBootstrapResetRunner(AdminSecurityProperties adminSecurityProperties,
                                             AdminAuthService adminAuthService) {
        this.adminSecurityProperties = adminSecurityProperties;
        this.adminAuthService = adminAuthService;
    }

    @Override
    public void run(ApplicationArguments args) {
        AdminSecurityProperties.BootstrapReset bootstrapReset = adminSecurityProperties.getBootstrapReset();
        if (bootstrapReset == null || !bootstrapReset.isEnabled()) {
            return;
        }

        String username = StringUtils.hasText(bootstrapReset.getUsername())
            ? bootstrapReset.getUsername().trim()
            : "admin";
        String password = bootstrapReset.getPassword();

        if (!StringUtils.hasText(password)) {
            log.warn("admin bootstrap password reset skipped because password is blank. targetUser={}", username);
            return;
        }

        try {
            adminAuthService.bootstrapResetPassword(username, password);
            log.warn("admin bootstrap password reset applied successfully. targetUser={}", username);
        } catch (BusinessException ex) {
            log.error("admin bootstrap password reset failed. targetUser={}, reason={}", username, ex.getMessage());
        }
    }
}
