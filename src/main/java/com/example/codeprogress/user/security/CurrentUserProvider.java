package com.example.codeprogress.user.security;

import com.example.codeprogress.user.User;
import com.example.codeprogress.user.exception.InvalidCredentialsException;
import com.example.codeprogress.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidCredentialsException();
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(InvalidCredentialsException::new);
    }
}
