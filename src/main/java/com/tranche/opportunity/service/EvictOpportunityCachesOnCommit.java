package com.tranche.opportunity.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Evicts opportunity caches after a successful commitment allocation.
 * Method parameter must be named {@code opportunityId}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
        @CacheEvict(cacheNames = OpportunityCacheNames.LIVE_LISTINGS, allEntries = true),
        @CacheEvict(cacheNames = OpportunityCacheNames.DETAIL, key = "#opportunityId")
})
public @interface EvictOpportunityCachesOnCommit {
}
