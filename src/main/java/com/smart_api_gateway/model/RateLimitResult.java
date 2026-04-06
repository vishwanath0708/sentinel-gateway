package com.smart_api_gateway.model;

public final class RateLimitResult {
    private final boolean allowed;
    private final long remainingTokens;

    public RateLimitResult(boolean allowed, long remainingTokens) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
    }

    public boolean allowed() { return allowed; }
    public long remainingTokens() { return remainingTokens; }

    // equals(), hashCode(), toString() auto-generated
}