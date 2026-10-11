package com.example.codeprogress.studysession.repository;

import com.example.codeprogress.studysession.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findAllByTopicIdOrderBySessionDateDesc(Long topicId);
}
