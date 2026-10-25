package com.example.codeprogress.integration;

import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.dto.CreateStudySessionRequest;
import com.example.codeprogress.studysession.service.StudySessionService;
import com.example.codeprogress.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "security.jwt.secret=test-only-jwt-secret-that-is-long-enough-for-hs256",
        "security.jwt.issuer=codeprogress-integration-tests",
        "security.jwt.access-token-ttl=PT1H",
        "app.time-zone=Europe/Moscow"
})
@AutoConfigureMockMvc
@Import(CodeProgressPostgresIT.FixedClockConfiguration.class)
class CodeProgressPostgresIT {

    private static final String PASSWORD = "integration-password";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("codeprogress_test")
            .withUsername("codeprogress")
            .withPassword("test-only-password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StudySessionService studySessionService;

    @Autowired
    private LearningTaskRepository learningTaskRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearApplicationData() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE study_session_tasks, study_sessions, learning_tasks, topics, users
                RESTART IDENTITY CASCADE
                """);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appliesAllFlywayMigrationsToRealPostgres() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success",
                Integer.class
        );
        Integer latestMigrationCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '10' AND success",
                Integer.class
        );
        String ownerNullable = jdbcTemplate.queryForObject(
                """
                        SELECT is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'topics'
                          AND column_name = 'owner_user_id'
                        """,
                String.class
        );
        String databaseProduct = jdbcTemplate.queryForObject("SELECT version()", String.class);

        assertThat(migrationCount).isGreaterThanOrEqualTo(10);
        assertThat(latestMigrationCount).isEqualTo(1);
        assertThat(ownerNullable).isEqualTo("NO");
        assertThat(databaseProduct).startsWith("PostgreSQL");
    }

    @Test
    void enforcesTopicOwnerInDatabaseAndCreatesTopicForAuthenticatedUser() throws Exception {
        String email = "topic-owner@example.com";
        register(email);
        String token = login(email);
        long userId = userRepository.findByEmail(email).orElseThrow().getId();

        long topicId = createTopic(token, "Owned topic");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT owner_user_id FROM topics WHERE id = ?",
                Long.class,
                topicId
        )).isEqualTo(userId);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO topics (title, description, status, created_at, updated_at, owner_user_id)
                VALUES ('Ownerless topic', NULL, 'PLANNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO topics (title, description, status, created_at, updated_at, owner_user_id)
                VALUES ('Unknown owner', NULL, 'PLANNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                """, Long.MAX_VALUE))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM topics", Integer.class)).isEqualTo(1);
    }

    @Test
    void supportsAuthenticationCrudAndUserDataIsolation() throws Exception {
        register(" OWNER@Example.com ");
        register("other@example.com");
        String ownerToken = login("owner@example.com");
        String otherToken = login("other@example.com");

        mockMvc.perform(get("/api/users/me").with(bearerToken(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "owner@example.com", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/topics"))
                .andExpect(status().isUnauthorized());

        long topicId = createTopic(ownerToken, "Java");
        mockMvc.perform(put("/api/topics/{id}", topicId)
                        .with(bearerToken(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Modern Java", "description", "Java 21"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Modern Java"));

        long taskId = createTask(ownerToken, topicId, "Records", TODAY.plusDays(3), "HIGH");
        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .with(bearerToken(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Records and sealed classes",
                                "description", "Practice domain modeling",
                                "difficulty", "HARD"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Records and sealed classes"));
        mockMvc.perform(patch("/api/tasks/{id}/planning", taskId)
                        .with(bearerToken(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("deadline", TODAY.plusDays(5), "priority", "LOW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value(TODAY.plusDays(5).toString()))
                .andExpect(jsonPath("$.priority").value("LOW"));

        mockMvc.perform(get("/api/tasks/{id}", taskId).with(bearerToken(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tasks/search").with(bearerToken(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        mockMvc.perform(delete("/api/tasks/{id}", taskId).with(bearerToken(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/tasks/{id}", taskId).with(bearerToken(ownerToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/topics/{id}", topicId).with(bearerToken(ownerToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/topics/{id}", topicId).with(bearerToken(ownerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateRegistrationAtDatabaseBackedApiLevel() throws Exception {
        register("duplicate@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "DUPLICATE@example.com", "password", PASSWORD))))
                .andExpect(status().isConflict());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    void updatingStudySessionRestoresStatusThatItPreviouslyManaged() throws Exception {
        register("student@example.com");
        String token = login("student@example.com");
        long topicId = createTopic(token, "Spring");
        long taskId = createTask(token, topicId, "Transactions", TODAY, "HIGH");
        updateTaskStatus(token, taskId, "IN_PROGRESS");

        long sessionId = createSession(token, topicId, TODAY, 45, Set.of(taskId));
        assertTaskStatus(token, taskId, "COMPLETED");

        mockMvc.perform(put("/api/sessions/{id}", sessionId)
                        .with(bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sessionDate", TODAY,
                                "durationMinutes", 60,
                                "note", "Theory only",
                                "completedTaskIds", Set.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.completedTaskIds").isEmpty());

        assertTaskStatus(token, taskId, "IN_PROGRESS");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM study_session_tasks WHERE study_session_id = ?",
                Integer.class,
                sessionId
        )).isZero();
    }

    @Test
    void deletingSessionsRestoresTaskOnlyAfterLastHistoricalReferenceIsRemoved() throws Exception {
        register("history@example.com");
        String token = login("history@example.com");
        long topicId = createTopic(token, "PostgreSQL");
        long taskId = createTask(token, topicId, "Indexes", TODAY, "MEDIUM");
        updateTaskStatus(token, taskId, "IN_PROGRESS");

        long firstSessionId = createSession(token, topicId, TODAY.minusDays(1), 30, Set.of(taskId));
        long secondSessionId = createSession(token, topicId, TODAY, 40, Set.of(taskId));
        assertTaskStatus(token, taskId, "COMPLETED");

        mockMvc.perform(delete("/api/sessions/{id}", firstSessionId).with(bearerToken(token)))
                .andExpect(status().isNoContent());
        assertTaskStatus(token, taskId, "COMPLETED");

        mockMvc.perform(delete("/api/sessions/{id}", secondSessionId).with(bearerToken(token)))
                .andExpect(status().isNoContent());
        assertTaskStatus(token, taskId, "IN_PROGRESS");
        mockMvc.perform(get("/api/sessions/{id}", secondSessionId).with(bearerToken(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rollsBackManagedTaskStatusWhenStudySessionInsertViolatesDatabaseConstraint() throws Exception {
        String email = "rollback@example.com";
        register(email);
        String token = login(email);
        long topicId = createTopic(token, "JPA");
        long taskId = createTask(token, topicId, "Atomic write", TODAY, "HIGH");
        updateTaskStatus(token, taskId, "IN_PROGRESS");
        long userId = userRepository.findByEmail(email).orElseThrow().getId();

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(email, "", List.of())
        );

        assertThatThrownBy(() -> studySessionService.create(
                topicId,
                new CreateStudySessionRequest(TODAY, -1, "Invalid duration", Set.of(taskId))
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(learningTaskRepository.findByIdAndTopicOwnerId(taskId, userId).orElseThrow().getStatus())
                .isEqualTo(LearningTaskStatus.IN_PROGRESS);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM study_sessions", Integer.class)).isZero();
    }

    @Test
    void dashboardAggregatesSessionOnceWhenItContainsSeveralCompletedTasks() throws Exception {
        register("analytics@example.com");
        register("outsider@example.com");
        String token = login("analytics@example.com");
        String outsiderToken = login("outsider@example.com");
        long topicId = createTopic(token, "Architecture");
        long firstTaskId = createTask(token, topicId, "Layers", TODAY, "MEDIUM");
        long secondTaskId = createTask(token, topicId, "DTOs", TODAY, "MEDIUM");
        createSession(token, topicId, TODAY, 60, Set.of(firstTaskId, secondTaskId));

        mockMvc.perform(get("/api/dashboard/summary").with(bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTopics").value(1))
                .andExpect(jsonPath("$.completedTasks").value(2))
                .andExpect(jsonPath("$.totalStudyMinutes").value(60))
                .andExpect(jsonPath("$.studySessionsCount").value(1));
        mockMvc.perform(get("/api/dashboard/activity")
                        .param("from", TODAY.toString())
                        .param("to", TODAY.toString())
                        .with(bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("Europe/Moscow"))
                .andExpect(jsonPath("$.days[0].date").value(TODAY.toString()))
                .andExpect(jsonPath("$.days[0].studyMinutes").value(60))
                .andExpect(jsonPath("$.days[0].studySessionsCount").value(1));
        mockMvc.perform(get("/api/dashboard/summary").with(bearerToken(outsiderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTopics").value(0))
                .andExpect(jsonPath("$.completedTasks").value(0))
                .andExpect(jsonPath("$.totalStudyMinutes").value(0));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM study_session_tasks WHERE study_session_id = (SELECT id FROM study_sessions)",
                Integer.class
        )).isEqualTo(2);
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private long createTopic(String token, String title) throws Exception {
        String response = mockMvc.perform(post("/api/topics")
                        .with(bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", title, "description", title + " topic"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createTask(
            String token,
            long topicId,
            String title,
            LocalDate deadline,
            String priority
    ) throws Exception {
        String response = mockMvc.perform(post("/api/topics/{topicId}/tasks", topicId)
                        .with(bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", title,
                                "description", title + " description",
                                "difficulty", "MEDIUM",
                                "deadline", deadline,
                                "priority", priority
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createSession(
            String token,
            long topicId,
            LocalDate date,
            int durationMinutes,
            Set<Long> completedTaskIds
    ) throws Exception {
        String response = mockMvc.perform(post("/api/topics/{topicId}/sessions", topicId)
                        .with(bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sessionDate", date,
                                "durationMinutes", durationMinutes,
                                "note", "Integration test session",
                                "completedTaskIds", completedTaskIds
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completedTaskIds", containsInAnyOrder(
                        completedTaskIds.stream().map(Long::intValue).toArray()
                )))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void updateTaskStatus(String token, long taskId, String statusValue) throws Exception {
        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .with(bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", statusValue))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(statusValue));
    }

    private void assertTaskStatus(String token, long taskId, String expectedStatus) throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", taskId).with(bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static RequestPostProcessor bearerToken(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock integrationTestClock() {
            return Clock.fixed(
                    Instant.parse("2026-10-20T09:00:00Z"),
                    ZoneId.of("Europe/Moscow")
            );
        }
    }
}
