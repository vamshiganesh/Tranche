package com.tranche.common.ratelimit;

import com.tranche.common.config.RateLimitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fixed-window rate limiter backed by Redis INCR + TTL.
 * Used to protect the commitment endpoint from accidental or abusive bursts.
 */
@Service
public class RateLimitService {

    private static final String KEY_PREFIX = "rate:commitment:";

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RateLimitService(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public boolean tryAcquireCommitment(Long investorUserId) {
        if (!properties.enabled()) {
            return true;
        }
        String key = KEY_PREFIX + investorUserId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, properties.window());
        }
        return count != null && count <= properties.requestsPerWindow();
    }

    public int limit() {
        return properties.requestsPerWindow();
    }

    public Duration window() {
        return properties.window();
    }
}
