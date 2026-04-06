package com.smart_api_gateway.monitor;


import com.smart_api_gateway.monitor.AdaptiveMetricsCollector;
import org.springframework.stereotype.Component;

/**
 * Decides how much to adjust refill rate based on system health.
 *
 * Reads:
 *   - Average latency
 *   - Error rate
 *
 * Returns adjusted refill rate.
 */
@Component
public class AdaptiveRateController {

    private final AdaptiveMetricsCollector metricsCollector;

    public AdaptiveRateController(AdaptiveMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /**
     * Adjust refill rate dynamically.
     *
     * @param baseRefillRate original tier refill rate
     * @return adjusted refill rate
     */
    public long adjustRefillRate(long baseRefillRate) {

        double avgLatency = metricsCollector.getAverageLatency();
        double errorRate = metricsCollector.getErrorRate();

        /*
         Decision matrix:
         - Healthy → normal refill
         - Moderate latency → reduce 30%
         - High latency → reduce 50%
         - High error rate → reduce aggressively
        */

        if (errorRate > 5) {
            // Critical error spike
            return Math.max(1, (long) (baseRefillRate * 0.25));
        }

        if (avgLatency > 400) {
            return Math.max(1, (long) (baseRefillRate * 0.5));
        }

        if (avgLatency > 200) {
            return Math.max(1, (long) (baseRefillRate * 0.7));
        }

        System.out.println("Latency: " + avgLatency +
                " ErrorRate: " + errorRate +
                " BaseRefill: " + baseRefillRate);


        // Healthy system
        return baseRefillRate;
    }
}