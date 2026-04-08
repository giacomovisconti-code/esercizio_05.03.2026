# Microservices Dependecies:
## Config Server
- Spring Web
- Spring Cloud Config Server
- Actuator
- Spring Cloud Netflix Eureka Client
- **Version**: Spring Boot 3.4.3

## Eureka Server
- Spring Cloud Netflix Eureka Server
- Actuator
- **Version**: Spring Boot 3.4.3

## API Gateway (Spring Cloud Gateway)
- Spring Cloud Gateway (WebFlux)
- Spring Cloud Netflix Eureka Discovery Client
- Actuator
- Spring Security
- **Redis Reactive** (for rate limiting & caching)
- **JWT (JJWT)** for token validation
- **Resilience4J** (circuit breaker support)
- **Version**: Spring Boot 3.4.5

## User Service
- Spring Web
- Spring Data JPA
- Spring Security
- MySQL Connector/J
- Actuator
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- SpringDoc OpenAPI (Swagger UI)
- Lombok
- **Version**: Spring Boot 3.4.3

## Product Service
- Spring Web
- Spring Data JPA
- MySQL Connector/J
- Actuator
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- SpringDoc OpenAPI (Swagger UI)
- **Redis** (caching for products by SKU)
- Spring Cloud Resilience4J (circuit breaker)
- Lombok
- **Version**: Spring Boot 3.4.3

## Inventory Service
- Spring Web
- Spring Data JPA
- MySQL Connector/J
- Actuator
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- SpringDoc OpenAPI (Swagger UI)
- **Redis** (caching for inventory by productId)
- Spring Cloud Resilience4J (circuit breaker)
- Lombok
- **Version**: Spring Boot 3.4.3

## Order Service
- Spring Web
- Spring Data JPA
- MySQL Connector/J
- Spring Cloud OpenFeign (service-to-service calls)
- Spring Cloud Resilience4J (circuit breaker for remote calls)
- Spring Kafka (Kafka Producer)
- Actuator
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- SpringDoc OpenAPI (Swagger UI)
- **Redis** (caching)
- Lombok
- **Version**: Spring Boot 3.4.3

## Notification Service
- Spring Kafka (Kafka Consumer)
- Actuator
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- Spring Data JPA
- MySQL Connector/J
- **Liquibase** (database migration management)
- Lombok
- **Version**: Spring Boot 3.4.3

# 📡 Server Ports & Network

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

### Creare un file secret.env con
# JWT Configuration
JWT_SECRET
JWT_EXPIRATION_MS

# MySQL Root User
MYSQL_ROOT_PASSWORD

# Database Users & Passwords
# IMPORTANTE: Questi valori DEVONO corrispondere a quelli nel file: mysql-init/01-init-db.sql
USERS_DB_PASS
ORDERS_DB_PASS
PRODUCTS_DB_PASS
NOTIFICATION_DB_PASS
INVENTORY_DB_PASS


## Avvio Progetto

### 1. Build JAR dei microservizi
Dalla root progetto:
```bash
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
docker build -t config-server:latest ./config-server
```

### 3. Avvio Stack con Docker Compose
```bash
docker compose up -d
```

# Endpoints

Tutti gli endpoint applicativi sono esposti tramite API Gateway (`http://localhost:8080`) con prefisso `/api/{service-name}`.
Molti di questi (le Index) ritornano liste paginate di records. L'endopoint accetta query params: page, pageSize:
- page: quante pagine di records voglio,
- pageSize: quanti records per pagina riceverò.
La paginazione è così composta: 
- { content: [ ], pageable: { pageNumber, pageSize, sort, offset, paged, unpaged }, last, totalPages, totalElements, first, size, number, sort, numberOfElements, empty }

### Gateway Routing

- `USER-SERVICE` -> `/api/users-service/**`
- `PRODUCT-SERVICE` -> `/api/products-service/**`
- `INVENTORY-SERVICE` -> `/api/inventory-service/**`
- `ORDER-SERVICE` -> `/api/orders-service/**`
- `NOTIFICATION-SERVICE` -> `/api/notification-service/**`
---



### User Service (`/api/users-service`)

#### Auth
- `POST /auth/login` (public)
    - request body: { username, password }
    - response: { token, role, username }

#### Users
- `GET /users/all` (admin only)
    - request: query params ( page, pageSize ) non obbligatori
    - response: { userId, username, role, created_at }, paginato
- `POST /users/register` (public)
    - request body: { username, password }
    - response: stringa
- `PATCH /users/giveAdmin/{userId}`
    - request static params: userId, UUID
    - response: stringa
---

### Product Service (`/api/products-service`)

- `GET /products/all` (public)
    - request: query params ( page, pageSize ) non obbligatori
    - response: { id, sku, name, description, price }, paginato
- `GET /products/{sku}` (public)
    - request static params: { id, sku, name, description, price } dato cachiato su redis
- `GET /products/search?name={name}` (public)
    - request: query params ( page, pageSize ) non obbligatori
    - response: { id, sku, name, description, price }, paginato
- `POST /products/create` (admin only)
    - request body: { sku, name, price, description }
    - response: stringa
- `PATCH /products/update` (admin only)
    - request body: { sku, name, price, description }
    - response: stringa
- `DELETE /products/delete/{sku}` (admin only)
    - request static params: sku, UUID
    - response: stringa

---
### Inventory Service (`/api/inventory-service`)

- `GET /inventory` (admin only)
    - request: query params ( page, pageSize ) non obbligatori
    - response: { id, sku, name, description, price }, paginato
- `GET /inventory/{productId}` (admin only) dato cachiato su redis
    - request static param: productId, UUID
    - response: { sku, quantity, updatedAt }
- `POST /inventory/create/{productId}` (admin only) auto-crea voce inventario con quantity=0 alla creazione del nuovo prodotto
- `PATCH /inventory/deduction` auto-deduce quantità (es. dopo ordine) con controllo che quantity non diventi negativa
    - request body:  [ { sku, quantity } ]
    - response: stringa
- `PATCH /inventory/addition` auto-aumenta quantità (es. dopo modifica ordine)
    - request body:  [ { sku, quantity } ]
    - response: stringa
- `PATCH /inventory/update` (admin only)
    - request body: { sku, quantity }
    - response: stringa 
- `DELETE /inventory/delete/{productId}` (admin only)
    - request static param: productId, UUID
    - response: stringa

---

### Order Service (`/api/orders-service`)

- `GET /orders` (admin only)
    - request: query params ( page, pageSize ) non obbligatori
    - response: { id, userId, total, orderStatus, createdAt, orderItems [ ], active, deleted }, paginato
- `GET /orders/{id}` (admin only o user creatore dell'ordine)
    - request static params id, UUID
    - response: { id, userId, total, orderStatus, createdAt, orderItems [ ], active, deleted }
- `POST /orders/create`  (user autenticato)
    - request body: { [{ sku, quantity }] }
    - response: { id, userId, total, orderStatus, createdAt, orderItems [ ], active, deleted }
  Richiede header interno `X-User-Id` (propagato dal Gateway dopo validazione JWT)
- `PATCH /orders/update/{orderId}`
    - request body: { [{ sku, quantity }] }
    - request static params: id, UUID
    - response: { id, userId, total, orderStatus, createdAt, orderItems [ ], active, deleted }
- `PATCH /orders/changestatus/{orderId}?status={status}` (admin only)
    - request static param: orderId, UUID
    - request query param: status, stringa
    - response: { id, userId, total, orderStatus, createdAt, orderItems [ ], active, deleted }
- `PATCH /orders/deactivate/{orderId}` (soft delete, admin only)
    - request static param: orderId, UUID
    - response: { id, userId, total, orderStatus, createdAt, orderItems [ ], active, deleted }
- `PATCH /orders/reactivate/{orderId}` (admin only)
    - request static param: orderId, UUID
    - response: { id, userId, total, orderStatus, createdAt, orderItems [ ], active, deleted }
- `PATCH /orders/delete/{orderId}`
    - request static param: orderId, UUID
    -  response: stringa
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
- vincolo unique su (`order_id`, `sku`) *(come definito nell’entity attuale)*

### notification-service (`messages`)
- `id` (Long, PK)
- `message_text` (String)
- `sended_at` (LocalDateTime, creation timestamp)

## Credenziali di test:
- User:
    {
      "username":"user1",
      "password":"Password1-"
    }
  - Admin:
    {
      "username":"admin1",
      "password":"Password1-"
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

