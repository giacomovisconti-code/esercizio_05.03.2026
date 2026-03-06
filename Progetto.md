                +--------------------+
                |      API Gateway   |
                |  JWT Validation    |
                +---------+----------+
                          |
        +-----------------+------------------+
        |                 |                  |
+----v----+      +-----v------+      +----v------+
| User    |      | Product    |      | Inventory |
| Service |      | Service    |      | Service   |
+----+----+      +-----+------+      +----+------+
|                 |                  |
|                 |                  |
|           +-----v------------------v-----+
|           |         Order Service        |
|           |  REST + Circuit Breaker      |
|           +-------------+---------------+
|                         |
|                     Kafka
|                         |
|                 +-------v-------+
|                 | Notification  |
|                 | Service       |
|                 +---------------+


         Infrastructure:
         - Config Server
         - Eureka
         - Kafka + Zookeeper
         - PostgreSQL / MySQL


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