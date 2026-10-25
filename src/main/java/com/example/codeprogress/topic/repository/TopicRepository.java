package com.example.codeprogress.topic.repository;

import com.example.codeprogress.topic.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByIdAndOwnerId(Long id, Long ownerId);

    List<Topic> findAllByOwnerId(Long ownerId);

    Page<Topic> findAllByOwnerId(Long ownerId, Pageable pageable);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}
