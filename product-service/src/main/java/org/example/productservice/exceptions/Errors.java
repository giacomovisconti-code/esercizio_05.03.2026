package org.example.productservice.exceptions;

public enum Errors {

    INVALID_PRODUCT_PRICE("Prezzo non valido"),
    PRODUCT_NOT_FOUND("Prodotto non trovato"),
    PRODUCT_ALREADY_REGISTERED("Prodotto già registrato"),
    STOCK_NOT_CREATED("La giacenza per il nuovo prodotto non è stata creata")
    ;

    private final String message;

    Errors (String message) { this.message = message; }

    public String key() { return  name(); }
    public String message() { return message; }
}
