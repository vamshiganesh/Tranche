package com.tranche.common.config;

import com.tranche.opportunity.service.OpportunityCacheNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisJsonSerializer,
            @Value("${tranche.cache.opportunity-list-ttl:60s}") Duration opportunityListTtl,
            @Value("${tranche.cache.opportunity-detail-ttl:30s}") Duration opportunityDetailTtl
    ) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(redisJsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                OpportunityCacheNames.LIVE_LISTINGS,
                defaults.entryTtl(opportunityListTtl),
                OpportunityCacheNames.DETAIL,
                defaults.entryTtl(opportunityDetailTtl)
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
