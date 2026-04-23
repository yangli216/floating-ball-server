package com.regionalai.floatingball.server.security;

import com.regionalai.floatingball.server.modules.device.entity.AiDevice;

public final class DeviceContextHolder {

    private static final ThreadLocal<AiDevice> HOLDER = new ThreadLocal<AiDevice>();

    private DeviceContextHolder() {
    }

    public static void set(AiDevice device) {
        HOLDER.set(device);
    }

    public static AiDevice get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
