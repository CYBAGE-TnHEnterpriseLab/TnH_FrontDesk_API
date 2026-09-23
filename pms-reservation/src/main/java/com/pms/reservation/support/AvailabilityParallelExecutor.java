package com.pms.reservation.support;

import com.pms.reservation.config.AvailabilityPerformanceProperties;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Runs availability fan-out work on bounded pools while propagating the caller's request and
 * security context (downstream clients read the inbound Authorization header from those holders).
 *
 * <p>Two pools are used on purpose: day-level tasks fan out into IO tasks, and sharing a single
 * bounded pool for both levels could deadlock.
 */
@Component
public class AvailabilityParallelExecutor {

    private final AvailabilityPerformanceProperties properties;
    private final ExecutorService dayExecutor;
    private final ExecutorService ioExecutor;

    public AvailabilityParallelExecutor(AvailabilityPerformanceProperties properties) {
        this.properties = properties;
        int parallelism = Math.max(1, properties.getParallelism());
        this.dayExecutor = Executors.newFixedThreadPool(parallelism, threadFactory("availability-day-"));
        this.ioExecutor = Executors.newFixedThreadPool(parallelism * 2, threadFactory("availability-io-"));
    }

    /** Maps the outer (per stay-date) fan-out. */
    public <S, T> List<T> mapDays(List<S> items, Function<S, T> mapper) {
        return map(items, mapper, dayExecutor);
    }

    /** Maps inner downstream-call fan-outs (inventory rows, per room-type rate quotes, ...). */
    public <S, T> List<T> mapIo(List<S> items, Function<S, T> mapper) {
        return map(items, mapper, ioExecutor);
    }

    public <T> CompletableFuture<T> submitIo(Supplier<T> supplier) {
        if (!properties.isParallelEnabled()) {
            return CompletableFuture.completedFuture(supplier.get());
        }
        Supplier<T> contextAware = contextAware(supplier);
        try {
            return CompletableFuture.supplyAsync(contextAware, ioExecutor);
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            return CompletableFuture.completedFuture(supplier.get());
        }
    }

    public <T> T join(CompletableFuture<T> future) {
        try {
            return future.get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Availability lookup interrupted", ex);
        } catch (ExecutionException ex) {
            throw rethrow(ex.getCause());
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new IllegalStateException("Availability lookup timed out", ex);
        }
    }

    private <S, T> List<T> map(List<S> items, Function<S, T> mapper, ExecutorService executor) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        if (!properties.isParallelEnabled() || items.size() == 1) {
            return items.stream().map(mapper).toList();
        }

        List<CompletableFuture<T>> futures;
        try {
            futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(contextAware(() -> mapper.apply(item)), executor))
                .toList();
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            return items.stream().map(mapper).toList();
        }

        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            futures.forEach(future -> future.cancel(true));
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Availability lookup interrupted", ex);
        } catch (ExecutionException ex) {
            futures.forEach(future -> future.cancel(true));
            throw rethrow(ex.getCause());
        } catch (TimeoutException ex) {
            futures.forEach(future -> future.cancel(true));
            throw new IllegalStateException("Availability lookup timed out", ex);
        }

        return futures.stream().map(CompletableFuture::join).toList();
    }

    private <T> Supplier<T> contextAware(Supplier<T> supplier) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return () -> {
            RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
            SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
            try {
                if (requestAttributes != null) {
                    RequestContextHolder.setRequestAttributes(requestAttributes, true);
                }
                if (securityContext != null) {
                    SecurityContextHolder.setContext(securityContext);
                }
                return supplier.get();
            } finally {
                if (previousAttributes == null) {
                    RequestContextHolder.resetRequestAttributes();
                } else {
                    RequestContextHolder.setRequestAttributes(previousAttributes);
                }
                SecurityContextHolder.setContext(previousSecurityContext);
            }
        };
    }

    private RuntimeException rethrow(Throwable cause) {
        Throwable unwrapped = cause instanceof CompletionException ? cause.getCause() : cause;
        if (unwrapped instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (unwrapped instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(unwrapped);
    }

    private ThreadFactory threadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @PreDestroy
    void shutdown() {
        dayExecutor.shutdownNow();
        ioExecutor.shutdownNow();
    }
}
