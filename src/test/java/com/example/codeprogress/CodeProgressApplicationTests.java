package com.example.codeprogress;

import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.topic.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class CodeProgressApplicationTests {

    @MockitoBean
    private TopicRepository topicRepository;

    @MockitoBean
    private LearningTaskRepository learningTaskRepository;

    @MockitoBean
    private StudySessionRepository studySessionRepository;

    @Test
    void contextLoads() {
    }
}
