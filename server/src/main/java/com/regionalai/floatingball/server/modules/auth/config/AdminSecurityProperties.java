package com.regionalai.floatingball.server.modules.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "floating-ball.admin")
public class AdminSecurityProperties {

    private BootstrapReset bootstrapReset = new BootstrapReset();

    public BootstrapReset getBootstrapReset() {
        return bootstrapReset;
    }

    public void setBootstrapReset(BootstrapReset bootstrapReset) {
        this.bootstrapReset = bootstrapReset;
    }

    public static class BootstrapReset {

        private boolean enabled;
        private String username = "admin";
        private String password = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
