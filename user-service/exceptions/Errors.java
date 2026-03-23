package org.example.orderservice.exceptions;

public enum Errors {
    PRODUCT_VALIDATION_NOT_FOUND("Prodotto non esistente"),
    STOCK_VALIDATION_NOT_ENOUGH("Quantità prodotto richiesta inferiore alla giacenza"),
    ORDER_NOT_FOUND("Ordine non trovato"),
    USER_NOT_ALLOWED_FOR_ORDER("Non hai i permessi per visualizzare questa risorsa"),
    INVALID_USERID("UserId non presente"),
    ORDER_ELIMINATED("Ordine eliminato, creane uno nuovo!"),
    ORDER_DEACTIVATED("Ordine disattivato, riattivalo per applicare le modifiche!"),
    ORDER_STATUS_INCOMPATIBLE("Stato ordine non compatibile con la richiesta"),
    ORDER_ALREADY_ACTIVE("Ordine già attivo"),
    PRODUCT_SERVICE_DOWN("Product service down per la validazione dell'ordine"),
    INVENTORY_SERVICE_DOWN("Inventory service down per la validazione dell'ordine")
    ;
    private final String message;

    Errors(String message) { this.message = message;}

    public String key(){ return name(); }

    public String message(){return message; }
}
