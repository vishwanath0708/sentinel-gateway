package com.smart_api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    @Bean
    public DefaultRedisScript<List> tokenBucketScript() {

        String lua = """
                
                local key = KEYS[1]

                local capacity = tonumber(ARGV[1])
                local refillRate = tonumber(ARGV[2])
                local currentTime = tonumber(ARGV[3])
                local requested = tonumber(ARGV[4])

                local bucket = redis.call("HMGET", key, "tokens", "lastRefill")

                local tokens = tonumber(bucket[1])
                local lastRefill = tonumber(bucket[2])

                if tokens == nil then
                    tokens = capacity
                    lastRefill = currentTime
                end

                local delta = math.max(0, currentTime - lastRefill)
                local refill = delta * refillRate
                tokens = math.min(capacity, tokens + refill)

                local allowedFlag = 0

                if tokens < requested then
                    allowedFlag = 0
                else
                    tokens = tokens - requested
                    allowedFlag = 1
                end

                redis.call("HMSET", key, "tokens", tokens, "lastRefill", currentTime)

                -- Prevent memory leak
                redis.call("EXPIRE", key, 120)

                return {allowedFlag, tokens}
                """;

        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(List.class);

        return script;
    }
}