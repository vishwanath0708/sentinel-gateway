package com.smart_api_gateway.controller;

import com.smart_api_gateway.model.RateLimitRequest;
import com.smart_api_gateway.model.RateLimitResponse;
import com.smart_api_gateway.model.RateLimitResult;
import com.smart_api_gateway.service.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/shouldAllow")
    public ResponseEntity<RateLimitResponse> shouldAllow(
            @RequestBody RateLimitRequest request) {

        RateLimitResult result =
                rateLimiterService.allowRequest(
                        request.getClientId(),
                        request.getTier()
                );

        RateLimitResponse response =
                new RateLimitResponse(
                        result.allowed(),
                        result.remainingTokens()
                );

        if (!result.allowed()) {
            return ResponseEntity.status(429).body(response);
        }

        return ResponseEntity.ok(response);
    }
}