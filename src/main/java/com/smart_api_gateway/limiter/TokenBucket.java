package com.smart_api_gateway.limiter;

import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket {

    private final long capacity;
    private final long refillRatePerSecond;

    private long availableTokens;
    private long lastRefillTimeNanos;

    private final ReentrantLock lock = new ReentrantLock();

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    public TokenBucket(long capacity, long refillRatePerSecond) {
        if (capacity <= 0 || refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("Capacity and refill rate must be positive");
        }

        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.availableTokens = capacity; // start full
        this.lastRefillTimeNanos = System.nanoTime();
    }

    public boolean tryConsume(long tokens) {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Tokens to consume must be positive");
        }

        lock.lock();
        try {
            refill();

            if (availableTokens >= tokens) {
                availableTokens -= tokens;
                return true;
            }

            return false;

        } finally {
            lock.unlock();
        }
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillTimeNanos;

        if (elapsedNanos <= 0) {
            return;
        }

        long tokensToAdd = (elapsedNanos * refillRatePerSecond) / NANOS_PER_SECOND;

        if (tokensToAdd > 0) {
            availableTokens = Math.min(capacity, availableTokens + tokensToAdd);

            // Calculate how much time we actually converted into tokens
            long nanosConsumed = (tokensToAdd * NANOS_PER_SECOND) / refillRatePerSecond;

            // Move lastRefillTime forward only by consumed time
            lastRefillTimeNanos += nanosConsumed;
        }
    }

    public long getAvailableTokens() {
        lock.lock();
        try {
            refill();
            return availableTokens;
        } finally {
            lock.unlock();
        }
    }
}
