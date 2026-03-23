package org.example.inventoryservice.exceptions.handler;

public enum Errors {
    STOCK_NOT_FOUND("Non esiste un prodotto con questo ProductID"),
    STOCK_ALREADY_REGISTERED("Prodotto già inventariato"),
    INVALID_STOCK_QTY("La quantità inserita è inferiore o uguale a zero"),
    NOT_ENOUGH_STOCK("La quantità richiesta è inferiore alla giacenza."),
    NO_ITEMS("Nessuno stock da modificare inserito")
    ;
    private final String message;

    Errors(String message) { this.message = message ;}

    public String key(){ return name(); }
    public String message(){ return message;}

}
