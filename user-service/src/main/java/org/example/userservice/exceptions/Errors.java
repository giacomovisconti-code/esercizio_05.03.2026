package org.example.userservice.exceptions;

public enum Errors {
    USERNAME_ALREADY_REGISTERED("Username già in uso"),
    USER_NOT_FOUND("User non trovato"),
    WRONG_PASSWORD("Password errata")

    ;
    private final String message;

    Errors(String message) { this.message = message;}

    public String key(){ return name(); }

    public String message(){return message; }
}
