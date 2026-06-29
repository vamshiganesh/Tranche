package com.tranche.common.config;

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableConfigurationProperties(SpringDataWebProperties.class)
public class WebConfig {

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer(
            PaginationProperties paginationProperties
    ) {
        return resolver -> {
            resolver.setMaxPageSize(paginationProperties.maxPageSize());
            resolver.setFallbackPageable(
                    org.springframework.data.domain.PageRequest.of(
                            0,
                            paginationProperties.defaultPageSize()
                    )
            );
        };
    }
}
