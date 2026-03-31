package org.example.apigateway.errors.handler;

public enum Errors {
    INVALID_TOKEN("Token non valido o scaduto"),
    UNAUTHORIZED("Non sei autorizzato per accedere a questa risorsa"),
    NO_TOKEN("Token non presente"),
    TOO_MANY_REQUEST("Stai effettuando troppe richieste")
    ;
    private final String message;

    Errors(String message) {
        this.message = message;
    }
    public String key(){ return name(); }

    public String message() { return message; }

}
