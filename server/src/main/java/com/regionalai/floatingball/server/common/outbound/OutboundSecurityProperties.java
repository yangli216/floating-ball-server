package com.regionalai.floatingball.server.common.outbound;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "floating-ball.outbound-security")
public class OutboundSecurityProperties {

    private List<String> allowedHosts = new ArrayList<String>();
    private boolean allowPrivateNetwork;
    private boolean allowInsecureHttp;
    private boolean allowProxyFakeIp;
    private int rateLimitPerMinute = 120;
    private int circuitFailureThreshold = 5;
    private long circuitOpenMs = 30000L;

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts == null ? new ArrayList<String>() : allowedHosts;
    }

    public boolean isAllowPrivateNetwork() {
        return allowPrivateNetwork;
    }

    public void setAllowPrivateNetwork(boolean allowPrivateNetwork) {
        this.allowPrivateNetwork = allowPrivateNetwork;
    }

    public boolean isAllowInsecureHttp() {
        return allowInsecureHttp;
    }

    public void setAllowInsecureHttp(boolean allowInsecureHttp) {
        this.allowInsecureHttp = allowInsecureHttp;
    }

    public boolean isAllowProxyFakeIp() {
        return allowProxyFakeIp;
    }

    public void setAllowProxyFakeIp(boolean allowProxyFakeIp) {
        this.allowProxyFakeIp = allowProxyFakeIp;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public int getCircuitFailureThreshold() {
        return circuitFailureThreshold;
    }

    public void setCircuitFailureThreshold(int circuitFailureThreshold) {
        this.circuitFailureThreshold = circuitFailureThreshold;
    }

    public long getCircuitOpenMs() {
        return circuitOpenMs;
    }

    public void setCircuitOpenMs(long circuitOpenMs) {
        this.circuitOpenMs = circuitOpenMs;
    }
}
