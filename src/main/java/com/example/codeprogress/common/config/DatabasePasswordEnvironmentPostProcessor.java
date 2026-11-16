package com.example.codeprogress.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

public class DatabasePasswordEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String REQUIRED_PASSWORD_MESSAGE =
            "Required environment variable DB_PASSWORD must be set and not blank";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {
        String databasePassword = environment.getProperty("DB_PASSWORD");
        if (!StringUtils.hasText(databasePassword)) {
            throw new IllegalStateException(REQUIRED_PASSWORD_MESSAGE);
        }
    }
}
