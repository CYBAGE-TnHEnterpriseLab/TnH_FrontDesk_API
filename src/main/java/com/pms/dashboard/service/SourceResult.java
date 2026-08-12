package com.pms.dashboard.service;

public record SourceResult<T>(String status, T payload) {

    public static <T> SourceResult<T> ok(T payload) {
        return new SourceResult<>("OK", payload);
    }

    public static <T> SourceResult<T> degraded(T payload) {
        return new SourceResult<>("DEGRADED", payload);
    }
}

