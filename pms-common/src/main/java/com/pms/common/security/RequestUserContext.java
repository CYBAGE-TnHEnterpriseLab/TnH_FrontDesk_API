package com.pms.common.security;

public final class RequestUserContext {

    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    private RequestUserContext() {
    }

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void clear() {
        USERNAME.remove();
    }
}
