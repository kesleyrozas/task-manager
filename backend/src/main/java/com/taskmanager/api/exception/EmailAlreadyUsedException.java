package com.taskmanager.api.exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("E-mail já está em uso: " + email);
    }
}
