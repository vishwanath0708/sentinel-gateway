package com.smart_api_gateway.monitor;



import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects runtime metrics for adaptive throttling.
 *
 * This class acts as a "sensor" for system health.
 * It measures:
 *   - Total number of requests
 *   - Total number of errors
 *   - Total accumulated latency
 *
 * Every 10 seconds, metrics reset to create a sliding health window.
 */
@Component
public class AdaptiveMetricsCollector {

    // Total number of requests in current 10s window
    private final AtomicLong totalRequests = new AtomicLong();

    // Total number of error responses in current window
    private final AtomicLong totalErrors = new AtomicLong();

    // Sum of latencies of all requests in current window
    private final AtomicLong totalLatency = new AtomicLong();

    /**
     * Call this at the end of every request.
     *
     * @param latencyMillis How long request took
     * @param errorOccurred Whether it was a system error (5xx)
     */
    public void recordRequest(long latencyMillis, boolean errorOccurred) {

        // Increase total request count
        totalRequests.incrementAndGet();

        // Add latency to cumulative latency
        totalLatency.addAndGet(latencyMillis);

        // If error happened, increase error counter
        if (errorOccurred) {
            totalErrors.incrementAndGet();
        }
    }

    /**
     * Returns average latency (ms) in last 10s window.
     */
    public double getAverageLatency() {
        long requests = totalRequests.get();

        if (requests == 0) {
            return 0;
        }

        // Average = totalLatency / totalRequests
        return (double) totalLatency.get() / requests;
    }

    /**
     * Returns error rate percentage (0–100)
     */
    public double getErrorRate() {
        long requests = totalRequests.get();

        if (requests == 0) {
            return 0;
        }

        return ((double) totalErrors.get() / requests) * 100;
    }

    /**
     * Resets metrics every 10 seconds.
     * This creates a rolling health window.
     */
    @Scheduled(fixedRate = 10000)
    public void resetMetrics() {
        totalRequests.set(0);
        totalErrors.set(0);
        totalLatency.set(0);
    }
}