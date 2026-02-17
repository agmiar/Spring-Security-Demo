package com.app.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLoginAttemptService {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration
            LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration
            ATTEMPT_WINDOW = Duration.ofMinutes(20);

    public RedisLoginAttemptService(
            StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void loginFailed(String username) {
        String attemptsKey = "login:attempts:" + username;
        String lockKey = "login:lock:" + username;

        Long attempts = redisTemplate
                .opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, ATTEMPT_WINDOW);
        }
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue()
                    .set(lockKey, "LOCKED", LOCK_DURATION);
        }
    }

    public void loginSucceeded(String username) {
        redisTemplate.delete("login:attempts:" + username);
        redisTemplate.delete("login:lock:" + username);
    }

    public boolean isBlocked(String username) {
        return redisTemplate.hasKey("login:lock:" + username);
    }
}
