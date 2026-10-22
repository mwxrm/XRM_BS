package com.xrm.xrm_bs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationCodeStore {

    private final RedisTemplate<String, String> redisTemplate;

    public String generateAndStoreMathResult(int result) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("math:" + token, String.valueOf(result), Duration.ofMinutes(5));
        return token;
    }

    public boolean verifyMath(String token, int userAnswer) {
        String stored = redisTemplate.opsForValue().get("math:" + token);
        if (stored == null) return false;
        // 不再删除 token，允许在有效期内重复验证
        return Integer.parseInt(stored) == userAnswer;
    }

    public void storeEmailCode(String email, String code) {
        redisTemplate.opsForValue().set("email:" + email, code, Duration.ofMinutes(5));
    }

    public boolean verifyEmailCode(String email, String code) {
        String stored = redisTemplate.opsForValue().get("email:" + email);
        if (stored == null || !stored.equals(code)) return false;
        redisTemplate.delete("email:" + email);
        return true;
    }
}