package com.tranche.opportunity.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Evicts LIVE listing and detail caches after an opportunity mutation.
 * Method parameter must be named {@code id}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
        @CacheEvict(cacheNames = OpportunityCacheNames.LIVE_LISTINGS, allEntries = true),
        @CacheEvict(cacheNames = OpportunityCacheNames.DETAIL, key = "#id")
})
public @interface EvictOpportunityCaches {
}
