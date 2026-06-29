package com.tranche.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfig {

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer(
            PaginationProperties paginationProperties
    ) {
        return resolver -> resolver.setMaxPageSize(paginationProperties.maxPageSize());
    }
}
