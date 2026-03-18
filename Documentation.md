# Microservices Dependecies:
## Config Server
    - Spring Web
    
    - Config Server
    
    - Actuator
    
    - Eureka Client

## Eureka Server
    - Eureka Server
    - Actuator
    - API Gateway

## Spring Cloud Gateway
    - Eureka Discovery Client
    - Actuator
    - Security

## User Service
    - Spring Web
    - Spring Data JPA
    - Spring Security
    - MySQL Driver
    -Actuator
    - Eureka Client
    - OpenAPI

## Product Service
    - Spring Web
    - Spring Data JPA
    - Database driver
    - Actuator
    - Eureka Client
    - OpenAPI

## Inventory Service
    - Spring Web

    - Spring Data JPA

    - Database driver

    - Actuator

    - Eureka Client

    - OpenAPI

## Order Service
    - Spring Web

    - Spring Data JPA

    - OpenFeign 

    - Resilience4J

    - Kafka

    - Actuator

    - Eureka Client

    - OpenAPI

## Notification Service
    - Spring Kafka
    - Actuator
    - Eureka Client

# Servers Ports:
- api-gateway: 8080
- config-server: 8888
- eureka-server: 8761
- inventory-service: 8081
- notification-service: 8082
- order-service: 8083
- product-service: 8084
- user-service: 8085


# Architettura Microservizi:
- 'config-server': configurazione centralizzata per tutti i microservizi,
- 'eureka-server': service discovery per registrare e scoprire i microservizi,
- 'api-gateway': punto di ingresso per tutte le richieste, con validazione JWT e routing verso i microservizi,
- 'user-service': gestione degli utenti e autenticazione
- 'product-service': gestione dei prodotti (al momento della creazione del prodotto, viene fatta una chiamata OpenFeign al servizio di inventario per creare una voce corrispondente nell'inventario)
- 'inventory-service': gestione dell'inventario
- 'order-service': gestione degli ordini, circuit breaker per gestire le chiamate OpenFeign dei servizi dipendenti e integrazione con Kafka per la comunicazione asincrona con il notification-service
- 'notification-service': gestione delle notifiche, con integrazione Kafka per ricevere il messaggio di conferma dell'ordine e inviare notifiche agli utenti (fake log)

## Prerequisiti:
- Java 17 
- Maven
- Docker
- MySQL

## Avvio Progetto

### 1. Build JAR dei microservizi
Dalla root progetto:

```bash
cd /home/giacomo/Scrivania/esercizio/esercizio_05.03.2026

cd config-server && ./mvnw clean package -DskipTests && cd ..
cd eureka-server && ./mvnw clean package -DskipTests && cd ..
cd api-gateway && ./mvnw clean package -DskipTests && cd ..
cd user-service && ./mvnw clean package -DskipTests && cd ..
cd product-service && ./mvnw clean package -DskipTests && cd ..
cd inventory-service && ./mvnw clean package -DskipTests && cd ..
cd order-service && ./mvnw clean package -DskipTests && cd ..
cd notification-service && ./mvnw clean package -DskipTests && cd ..
```

### 2. Build Immagini Docker
```bash
docker build -t eureka-server:latest ./eureka-server
docker build -t api-gateway:latest ./api-gateway
docker build -t user-service:latest ./user-service
docker build -t product-service:latest ./product-service
docker build -t inventory-service:latest ./inventory-service
docker build -t order-service:latest ./order-service
docker build -t notification-service:latest ./notification-service
```

### 3. Avvio Stack con Docker Compose
```bash
docker compose up -d
```

# Endpoints

Tutti gli endpoint applicativi sono esposti tramite API Gateway (`http://localhost:8080`) con prefisso `/api/{service-name}`.

### Gateway Routing

- `USER-SERVICE` -> `/api/users-service/**`
- `PRODUCT-SERVICE` -> `/api/products-service/**`
- `INVENTORY-SERVICE` -> `/api/inventory-service/**`
- `ORDER-SERVICE` -> `/api/orders-service/**`

---

### User Service (`/api/users-service`)

#### Auth
- `POST /auth/login` (public)
    - request body: { username, password }
    - response: { token, role, username }

#### Users
- `GET /users/all` (admin only)
- `POST /users/register` (public)
    - request body: { username, password }

---

### Product Service (`/api/products-service`)

- `GET /products/all` (public)
- `GET /products/{sku}` (public)
- `GET /products/search?name={name}` (public)
- `POST /products/create` (admin only)
    - request body: { sku, name, price, description }
- `PATCH /products/update` (admin only)
    - request body: { sku, name, price, description }
- `DELETE /products/delete/{sku}` (admin only)

---
### Inventory Service (`/api/inventory-service`)

- `GET /inventory` (admin only)
- `GET /inventory/{productId}` (admin only)
- `POST /inventory/create/{productId}` (admin only) auto-crea voce inventario con quantity=0 per nuovo prodotto
- `PATCH /inventory/deduction/{productId}?quantity={qty}` auto-deduce quantità (es. dopo ordine) con controllo che quantity non diventi negativa
- `PATCH /inventory/addition/{sku}?quantity={qty}` auto-aumenta quantità (es. dopo modifica ordine)
- `PATCH /inventory/update` (admin only)
    - request body: { sku, quantity }
- `DELETE /inventory/delete/{productId}` (admin only)

---

### Order Service (`/api/orders-service`)

- `GET /orders` (admin only)
- `GET /orders/{id}` (admin only o user se è suo ordine)
- `POST /orders/create`  (user autenticato)
    - request body: { [{ sku, quantity }] }
  Richiede header interno `X-User-Id` (propagato dal Gateway dopo validazione JWT)
- `PATCH /orders/update/{orderId}`
    - request body: { [{ sku, quantity }] }
- `PATCH /orders/changestatus/{orderId}?status={status}` (admin only)
- `PATCH /orders/deactivate/{orderId}` (soft delete, admin only)
- `PATCH /orders/reactivate/{orderId}` (admin only)
- `PATCH /orders/delete/{orderId}`

---

### Actuator

Ogni microservizio espone endpoint Actuator (almeno health), ad esempio:
- `/actuator/health`

## Schema Database

### user-service (`users`)
- `id` (UUID, PK)
- `username` (String, unique)
- `password` (String)
- `role` (Enum String: `ROLE_USER` / `ROLE_ADMIN`)
- `created_at` (LocalDateTime, creation timestamp)

### product-service (`products`)
- `id` (UUID, PK)
- `name` (String)
- `sku` (UUID, unique)
- `price` (BigDecimal)
- `description` (TEXT, nullable)
- `created_at` (LocalDateTime)
- `updated_at` (LocalDateTime)

### inventory-service (`inventory`)
- `id` (Long, PK)
- `sku` (UUID, unique, not null)
- `quantity` (Long)
- `updatedAt` (LocalDateTime)

### order-service (`orders`)
- `id` (UUID, PK)
- `userId` (UUID, not null)
- `total` (BigDecimal)
- `orderStatus` (Enum String)
- `createdAt` (LocalDateTime)
- `active` (Boolean)
- `deleted` (Boolean)
- relazione `1-N` con `order_items` (cascade all, orphan removal)

### order-service (`order_items`)
- `id` (Long, PK)
- `sku` (UUID)
- `quantity` (Long, min 1)
- `unitPrice` (BigDecimal)
- `order_id` (FK -> `orders.id`, not null)
- vincolo unique su (`order_id`, `product_id`) *(come definito nell’entity attuale)*

### notification-service (`messages`)
- `id` (Long, PK)
- `message_text` (String)
- `sended_at` (LocalDateTime, creation timestamp)

## Credenziali di test:
- User:
    {
      "username":"userTest",
      "password":"user1234"
    }
  - Admin:
    {
      "username":"user1",
      "password":"1234"
    }

## Acceptance Checklist

- [ ] `docker compose up -d` eseguito senza errori critici.
- [ ] `docker compose ps` mostra tutti i container principali `Up`:
  - [ ] config-server
  - [ ] eureka-server
  - [ ] api-gateway
  - [ ] mysql
  - [ ] zookeeper
  - [ ] kafka
  - [ ] user-service
  - [ ] product-service
  - [ ] inventory-service
  - [ ] order-service
  - [ ] notification-service

- [ ] Healthcheck servizi raggiungibili:
  - [ ] `GET http://localhost:8888/actuator/health` (config-server)
  - [ ] `GET http://localhost:8761` (eureka-server)
  - [ ] `GET http://localhost:8080/actuator/health` (gateway)

- [ ] Autenticazione OK:
  - [ ] login admin -> token JWT ricevuto
  - [ ] login user -> token JWT ricevuto

- [ ] Autorizzazione/ruoli OK:
  - [ ] endpoint admin-only bloccati per user
  - [ ] endpoint user-only bloccati per admin/non autenticato
  - [ ] endpoint protetti accettano Bearer token valido

- [ ] Product flow OK:
  - [ ] create product
  - [ ] get/list product
  - [ ] update/delete product

- [ ] Inventory flow OK:
  - [ ] update/set stock prodotto creato
  - [ ] lettura stock corretta

- [ ] Order end-to-end OK:
  - [ ] create order con utente autenticato
  - [ ] ordine persistito in DB (`orders`, `order_items`)
  - [ ] `GET /orders/{id}` restituisce dati corretti

- [ ] Messaggistica Kafka OK:
  - [ ] `order-service` pubblica evento `order.created`
  - [ ] `notification-service` consuma evento (verificato da log)