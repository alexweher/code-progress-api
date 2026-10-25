package com.example.codeprogress;

import com.example.codeprogress.dashboard.repository.DashboardRepository;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.topic.repository.TopicRepository;
import com.example.codeprogress.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "security.jwt.secret=test-secret-with-at-least-32-bytes-long",
        "security.jwt.issuer=codeprogress-api",
        "security.jwt.access-token-ttl=PT1H"
})
class CodeProgressApplicationTests {

    @MockitoBean
    private DashboardRepository dashboardRepository;

    @MockitoBean
    private TopicRepository topicRepository;

    @MockitoBean
    private LearningTaskRepository learningTaskRepository;

    @MockitoBean
    private StudySessionRepository studySessionRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void contextLoads() {
    }
}
