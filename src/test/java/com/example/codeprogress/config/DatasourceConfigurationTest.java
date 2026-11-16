package com.example.codeprogress.config;

import com.example.codeprogress.common.config.DatabasePasswordEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatasourceConfigurationTest {

    private static final String TEST_DATABASE_PASSWORD = "test-only-database-password";
    private static final String REQUIRED_PASSWORD_MESSAGE =
            "Required environment variable DB_PASSWORD must be set and not blank";

    private final DatabasePasswordEnvironmentPostProcessor postProcessor =
            new DatabasePasswordEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    void acceptsDatabasePasswordWhenProvided() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DB_PASSWORD", TEST_DATABASE_PASSWORD);

        postProcessor.postProcessEnvironment(environment, application);
    }

    @Test
    void rejectsMissingDatabasePasswordWithoutLoggingASecret() {
        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(new MockEnvironment(), application))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(REQUIRED_PASSWORD_MESSAGE)
                .hasMessageNotContaining(TEST_DATABASE_PASSWORD);
    }

    @Test
    void rejectsBlankDatabasePasswordWithoutLoggingItsValue() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DB_PASSWORD", "   ");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, application))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(REQUIRED_PASSWORD_MESSAGE)
                .hasMessageNotContaining(TEST_DATABASE_PASSWORD);
    }

    @Test
    void applicationConfigurationDoesNotDefineDatabasePasswordFallback() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(applicationYaml)
                .contains("password: ${DB_PASSWORD}")
                .doesNotContainPattern("\\$\\{DB_PASSWORD:[^}]+}");
    }

    @Test
    void registersEarlyDatabasePasswordValidation() throws IOException {
        String springFactories = Files.readString(
                Path.of("src/main/resources/META-INF/spring.factories")
        );

        assertThat(springFactories)
                .contains("org.springframework.boot.env.EnvironmentPostProcessor")
                .contains(DatabasePasswordEnvironmentPostProcessor.class.getName());
    }

    @Test
    void dockerBuildContextExcludesSensitiveLocalArtifactsWithoutExcludingBuildInputs() throws IOException {
        String dockerIgnore = Files.readString(Path.of(".dockerignore"));

        assertThat(dockerIgnore)
                .contains("backups")
                .contains("*.dump")
                .contains("*.sql")
                .contains(".env.*")
                .contains("*_REPORT.md")
                .contains("AGENTS.md")
                .doesNotContain("pom.xml")
                .doesNotContain("src");
    }

}
