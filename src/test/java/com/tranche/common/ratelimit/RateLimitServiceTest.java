package com.tranche.common.ratelimit;

import com.tranche.common.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(
                redisTemplate,
                new RateLimitProperties(true, 2, Duration.ofMinutes(1))
        );
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void allowsRequestsWithinLimit() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L);

        assertThat(rateLimitService.tryAcquireCommitment(42L)).isTrue();
        assertThat(rateLimitService.tryAcquireCommitment(42L)).isTrue();
        verify(redisTemplate).expire(eq("rate:commitment:42"), eq(Duration.ofMinutes(1)));
    }

    @Test
    void blocksWhenLimitExceeded() {
        when(valueOperations.increment(anyString())).thenReturn(3L);

        assertThat(rateLimitService.tryAcquireCommitment(42L)).isFalse();
    }

    @Test
    void disabledLimiterAlwaysAllows() {
        RateLimitService disabled = new RateLimitService(
                redisTemplate,
                new RateLimitProperties(false, 2, Duration.ofMinutes(1))
        );

        assertThat(disabled.tryAcquireCommitment(42L)).isTrue();
    }
}
