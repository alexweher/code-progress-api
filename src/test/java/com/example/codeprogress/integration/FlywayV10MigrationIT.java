package com.example.codeprogress.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class FlywayV10MigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("flyway_v10_test")
            .withUsername("codeprogress")
            .withPassword("test-only-password");

    @Test
    void createsFreshSchemaThroughV10() {
        String schema = newSchemaName();

        migrate(schema, "10");

        JdbcTemplate jdbcTemplate = jdbcTemplate();
        assertThat(successfulMigrationCount(jdbcTemplate, schema)).isEqualTo(10);
        assertThat(ownerColumnNullable(jdbcTemplate, schema)).isEqualTo("NO");
    }

    @Test
    void upgradesV9ToV10WhenExistingTopicHasOwner() {
        String schema = newSchemaName();
        migrate(schema, "9");
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        long ownerId = insertUser(jdbcTemplate, schema, "owned-topic@example.com");
        insertTopic(jdbcTemplate, schema, "Owned topic", ownerId);

        migrate(schema, "10");

        assertThat(successfulMigrationCount(jdbcTemplate, schema)).isEqualTo(10);
        assertThat(ownerColumnNullable(jdbcTemplate, schema)).isEqualTo("NO");
        assertThat(rowCount(jdbcTemplate, schema, "topics")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT owner_user_id FROM " + qualified(schema, "topics") + " WHERE title = ?",
                Long.class,
                "Owned topic"
        )).isEqualTo(ownerId);
    }

    @Test
    void rejectsV10WhenOwnerlessTopicExistsAndPreservesV9SchemaAndData() {
        String schema = newSchemaName();
        migrate(schema, "9");
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        insertTopic(jdbcTemplate, schema, "Legacy ownerless topic", null);

        assertThatThrownBy(() -> migrate(schema, "10"))
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("Cannot require topics.owner_user_id");

        assertThat(successfulMigrationCount(jdbcTemplate, schema)).isEqualTo(9);
        assertThat(ownerColumnNullable(jdbcTemplate, schema)).isEqualTo("YES");
        assertThat(rowCount(jdbcTemplate, schema, "topics")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + qualified(schema, "topics") + " WHERE owner_user_id IS NULL",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + qualified(schema, "flyway_schema_history")
                        + " WHERE version = '10' AND success",
                Integer.class
        )).isZero();
    }

    private void migrate(String schema, String target) {
        Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .schemas(schema)
                .defaultSchema(schema)
                .target(MigrationVersion.fromVersion(target))
                .load()
                .migrate();
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()
        ));
    }

    private long insertUser(JdbcTemplate jdbcTemplate, String schema, String email) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO %s (email, password_hash)
                VALUES (?, '$2a$10$testOnlyPasswordHashForMigrationTest000000000000000000')
                RETURNING id
                """.formatted(qualified(schema, "users")), Long.class, email);
    }

    private void insertTopic(JdbcTemplate jdbcTemplate, String schema, String title, Long ownerId) {
        jdbcTemplate.update("""
                INSERT INTO %s (title, description, status, owner_user_id)
                VALUES (?, NULL, 'PLANNED', ?)
                """.formatted(qualified(schema, "topics")), title, ownerId);
    }

    private int successfulMigrationCount(JdbcTemplate jdbcTemplate, String schema) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + qualified(schema, "flyway_schema_history")
                        + " WHERE success AND version IS NOT NULL",
                Integer.class
        );
    }

    private String ownerColumnNullable(JdbcTemplate jdbcTemplate, String schema) {
        return jdbcTemplate.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = ?
                  AND table_name = 'topics'
                  AND column_name = 'owner_user_id'
                """, String.class, schema);
    }

    private int rowCount(JdbcTemplate jdbcTemplate, String schema, String table) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + qualified(schema, table),
                Integer.class
        );
    }

    private static String newSchemaName() {
        return "v10_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String qualified(String schema, String objectName) {
        return '"' + schema + "\".\"" + objectName + '"';
    }
}
