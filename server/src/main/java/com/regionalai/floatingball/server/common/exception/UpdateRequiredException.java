package com.regionalai.floatingball.server.common.exception;

public class UpdateRequiredException extends RuntimeException {

    private final String minSupportedVersion;

    public UpdateRequiredException(String minSupportedVersion) {
        super("当前客户端版本过低，请升级到 " + minSupportedVersion + " 或更高版本后继续使用");
        this.minSupportedVersion = minSupportedVersion;
    }

    public String getMinSupportedVersion() {
        return minSupportedVersion;
    }
}
