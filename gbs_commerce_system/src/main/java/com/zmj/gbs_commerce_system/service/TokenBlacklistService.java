package com.zmj.gbs_commerce_system.service;

public interface TokenBlacklistService {

    void addToBlacklist(String token, long expirationMillis);

    boolean isBlacklisted(String token);
}
