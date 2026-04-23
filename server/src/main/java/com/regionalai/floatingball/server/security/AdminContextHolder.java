package com.regionalai.floatingball.server.security;

import com.regionalai.floatingball.server.modules.auth.dto.AdminCurrentUser;

public final class AdminContextHolder {

    private static final ThreadLocal<AdminCurrentUser> HOLDER = new ThreadLocal<AdminCurrentUser>();

    private AdminContextHolder() {
    }

    public static void set(AdminCurrentUser user) {
        HOLDER.set(user);
    }

    public static AdminCurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
