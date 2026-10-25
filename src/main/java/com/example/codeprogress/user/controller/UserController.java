package com.example.codeprogress.user.controller;

import com.example.codeprogress.common.exception.ApiErrorResponse;
import com.example.codeprogress.user.dto.UserResponse;
import com.example.codeprogress.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned"),
            @ApiResponse(responseCode = "401", description = "Bearer token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public UserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return userService.getByEmail(jwt.getSubject());
    }
}
