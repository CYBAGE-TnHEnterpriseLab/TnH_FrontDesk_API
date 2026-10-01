package com.pms.reservation.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public final class SimpleCache {

    private SimpleCache() {}

    public static <T> CachedValue<T> cached(Supplier<T> loader, Duration ttl) {
        return new CachedValue<>(loader, ttl);
    }

    public static final class CachedValue<T> {
        private volatile T value;
        private volatile Instant expiresAt = Instant.MIN;
        private final Supplier<T> loader;
        private final Duration ttl;

        public CachedValue(Supplier<T> loader, Duration ttl) {
            this.loader = loader;
            this.ttl = ttl;
        }

        public T get() {
            if (value == null || Instant.now().isAfter(expiresAt)) {
                synchronized (this) {
                    if (value == null || Instant.now().isAfter(expiresAt)) {
                        value = loader.get();
                        expiresAt = Instant.now().plus(ttl);
                    }
                }
            }
            return value;
        }

        public void invalidate() {
            synchronized (this) {
                value = null;
                expiresAt = Instant.MIN;
            }
        }
    }
}
