package com.smart_api_gateway.model;



public class RateLimitRequest {

    private String clientId;
    private String tier;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }
}

