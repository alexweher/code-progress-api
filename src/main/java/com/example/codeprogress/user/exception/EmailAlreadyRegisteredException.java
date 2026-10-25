package com.example.codeprogress.user.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("User with this email is already registered");
    }
}
