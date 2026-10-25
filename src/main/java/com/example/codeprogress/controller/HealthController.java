package com.example.codeprogress.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Health")
@SecurityRequirements
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Check whether the application is running", security = {})
    public HealthResponse health() {
        return new HealthResponse("UP", "CodeProgress");
    }

    public record HealthResponse(String status, String application) {
    }
}
