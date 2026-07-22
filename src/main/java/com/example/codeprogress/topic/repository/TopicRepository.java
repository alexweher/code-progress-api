package com.example.codeprogress.topic.repository;

import com.example.codeprogress.topic.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
}
