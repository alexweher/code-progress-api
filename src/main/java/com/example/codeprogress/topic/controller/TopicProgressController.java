package com.example.codeprogress.topic.controller;

import com.example.codeprogress.topic.dto.TopicProgressResponse;
import com.example.codeprogress.topic.service.TopicProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
public class TopicProgressController {

    private final TopicProgressService topicProgressService;

    public TopicProgressController(TopicProgressService topicProgressService) {
        this.topicProgressService = topicProgressService;
    }

    @GetMapping("/{topicId}/progress")
    public TopicProgressResponse getProgress(@PathVariable Long topicId) {
        return topicProgressService.getProgress(topicId);
    }
}
