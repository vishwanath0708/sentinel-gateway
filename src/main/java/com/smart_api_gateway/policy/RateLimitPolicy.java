package com.smart_api_gateway.policy;

public final class RateLimitPolicy {

    private final long capacity;
    private final long refillRatePerSecond;

    public RateLimitPolicy(long capacity, long refillRatePerSecond) {
        if (capacity <= 0 || refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("Capacity and refill rate must be positive");
        }
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getRefillRatePerSecond() {
        return refillRatePerSecond;
    }
}
