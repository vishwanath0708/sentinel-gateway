package com.smart_api_gateway.service;

import com.smart_api_gateway.model.RateLimitResult;
import com.smart_api_gateway.monitor.AdaptiveMetricsCollector;
import com.smart_api_gateway.monitor.AdaptiveRateController;
import com.smart_api_gateway.policy.FailureMode;
import com.smart_api_gateway.policy.RateLimitPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> script;

    @Autowired
    private AdaptiveMetricsCollector metricsCollector;

    @Autowired
    private AdaptiveRateController adaptiveRateController;


    private final AtomicLong totalAllowed = new AtomicLong();
    private final AtomicLong totalRejected = new AtomicLong();

    private volatile FailureMode failureMode = FailureMode.FAIL_CLOSED;


    // Global QPS counter
    private final AtomicLong globalRequestCounter = new AtomicLong();

    // Maximum allowed global requests per second
    private static final long MAX_GLOBAL_QPS = 10000; // adjust based on capacity

    private final Map<String, RateLimitPolicy> clientPolicies = new ConcurrentHashMap<>();

    public RateLimiterService(StringRedisTemplate redisTemplate,
                              DefaultRedisScript<List> script) {

        this.redisTemplate = redisTemplate;
        this.script = script;

        clientPolicies.put("FREE", new RateLimitPolicy(60, 1));
        clientPolicies.put("PRO", new RateLimitPolicy(600, 100));
        clientPolicies.put("VIP", new RateLimitPolicy(6000, 1000));
    }

    /**
     * Executes distributed token bucket algorithm atomically in Redis.
     */
    public RateLimitResult allowRequest(String clientId, String tier) {



        long startTime = System.currentTimeMillis();

        // 1️⃣ Global protection
        long currentGlobalCount = globalRequestCounter.incrementAndGet();

        if (currentGlobalCount > MAX_GLOBAL_QPS) {
            totalRejected.incrementAndGet();

            // Record latency before returning
            metricsCollector.recordRequest(
                    System.currentTimeMillis() - startTime,
                    false
            );

            return new RateLimitResult(false, 0);
        }

        try {



            RateLimitPolicy policy = resolvePolicy(tier);

            // 🔥 ADAPTIVE: Adjust refill rate dynamically
            long adjustedRefill =
                    adaptiveRateController.adjustRefillRate(
                            policy.getRefillRatePerSecond()
                    );

            String key = "rate_limit:" + clientId;

            long currentTime = System.currentTimeMillis() / 1000;

            List<?> result = redisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    String.valueOf(policy.getCapacity()),
                    String.valueOf(adjustedRefill), // adaptive rate here
                    String.valueOf(currentTime),
                    "1"
            );

            if (result == null || result.size() < 2) {
                throw new IllegalStateException("Invalid Lua response");
            }

            long allowedFlag = ((Number) result.get(0)).longValue();
            long remaining = ((Number) result.get(1)).longValue();

            boolean allowed = allowedFlag == 1;

            if (allowed) {
                totalAllowed.incrementAndGet();
            } else {
                totalRejected.incrementAndGet();
            }

            // Record request metrics
            metricsCollector.recordRequest(
                    System.currentTimeMillis() - startTime,
                    false
            );

            return new RateLimitResult(allowed, remaining);

        } catch (Exception e) {

            totalRejected.incrementAndGet();

            metricsCollector.recordRequest(
                    System.currentTimeMillis() - startTime,
                    true   // system error
            );

            return new RateLimitResult(false, 0);
        }
    }

    private RateLimitPolicy resolvePolicy(String tier) {
        RateLimitPolicy policy = clientPolicies.get(tier);
        if (policy == null) {
            throw new IllegalArgumentException("Unknown tier: " + tier);
        }
        return policy;
    }

    public long getTotalAllowed() {
        return totalAllowed.get();
    }

    public long getTotalRejected() {
        return totalRejected.get();
    }


    @Scheduled(fixedRate = 1000)
    public void resetGlobalCounter() {
        globalRequestCounter.set(0);
    }
}