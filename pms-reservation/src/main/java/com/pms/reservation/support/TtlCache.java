package com.pms.reservation.support;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Small TTL cache that also collapses concurrent loads of the same key into a single execution.
 * Failed loads are never cached.
 */
public class TtlCache<K, V> {

    private final ConcurrentHashMap<K, Entry<V>> store = new ConcurrentHashMap<>();
    private final long ttlMs;
    private final int maxSize;

    public TtlCache(long ttlMs, int maxSize) {
        this.ttlMs = ttlMs;
        this.maxSize = Math.max(1, maxSize);
    }

    public V get(K key, Supplier<V> loader) {
        if (ttlMs <= 0L) {
            return loader.get();
        }

        long now = System.currentTimeMillis();
        Entry<V> existing = store.get(key);
        if (existing != null && existing.expiresAt > now) {
            return await(existing);
        }

        Entry<V> candidate = new Entry<>(new CompletableFuture<>(), now + ttlMs);
        Entry<V> winner = store.compute(
            key,
            (ignoredKey, current) -> current != null && current.expiresAt > now ? current : candidate
        );

        if (winner != candidate) {
            return await(winner);
        }

        evictIfOversized(now);

        try {
            V value = loader.get();
            candidate.future.complete(value);
            return value;
        } catch (RuntimeException | Error ex) {
            store.remove(key, candidate);
            candidate.future.completeExceptionally(ex);
            throw ex;
        }
    }

    private V await(Entry<V> entry) {
        try {
            return entry.future.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw ex;
        }
    }

    private void evictIfOversized(long now) {
        if (store.size() <= maxSize) {
            return;
        }

        Iterator<Map.Entry<K, Entry<V>>> iterator = store.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, Entry<V>> entry = iterator.next();
            if (entry.getValue().expiresAt <= now) {
                iterator.remove();
            }
        }

        if (store.size() > maxSize) {
            store.clear();
        }
    }

    private static final class Entry<V> {
        private final CompletableFuture<V> future;
        private final long expiresAt;

        private Entry(CompletableFuture<V> future, long expiresAt) {
            this.future = future;
            this.expiresAt = expiresAt;
        }
    }
}
