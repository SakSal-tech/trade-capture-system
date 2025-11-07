package com.technicalchallenge.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// configures a ModelMapper bean for Spring application. ModelMapper is a
// library that automatically maps fields between objects, typically between
// DTOs and entities
public class ModelMapperConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                // MatchingStrategies.STRICT: Only maps fields with exactly matching names and
                // types.
                .setMatchingStrategy(MatchingStrategies.STRICT)
                // .setFieldMatchingEnabled(true): Allows mapping of fields directly, not just
                // via getters/setters.
                .setFieldMatchingEnabled(true)
                // .setFieldAccessLevel(PRIVATE): Permits access to private fields for mapping.
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
        return mapper;
    }

}
