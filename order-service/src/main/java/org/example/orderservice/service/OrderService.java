package org.example.orderservice.service;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import org.example.orderservice.dto.StockChange;
import org.example.orderservice.dto.order.OrderItemDraft;
import org.example.orderservice.dto.order.ItemToOrder;
import org.example.orderservice.dto.ProductDto;
import org.example.orderservice.dto.StockRequest;
import org.example.orderservice.entities.Order;
import org.example.orderservice.entities.OrderItems;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.exceptions.Errors;
import org.example.orderservice.exceptions.OrderException;
import org.example.orderservice.kafka.KafkaProducer;
import org.example.orderservice.openfeign.InventoryClient;
import org.example.orderservice.openfeign.ProductClient;
import org.example.orderservice.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {


    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private ProductClient productClient;

    private static final String MESSAGE = """
    Email di conferma per utente:  %s,
    
    Il tuo ordine con ID %s, è stato creato con successo!
""";

    //? UTILS
    // Circuit Breaker per product validation
    @CircuitBreaker(name = "validation", fallbackMethod = "fallBackProduct")
    private ProductDto productValidation(ItemToOrder itemToOrder) throws Exception {
        ProductDto p = productClient.getProduct(itemToOrder.getSku()).getBody();
        if (p == null) throw new OrderException(Errors.PRODUCT_VALIDATION_NOT_FOUND.key(),Errors.PRODUCT_VALIDATION_NOT_FOUND.message());
        return p;
    }

    // Circuit Breaker per stock validation
    @CircuitBreaker(name = "validation", fallbackMethod = "fallBackInventory")
    private void stockValidation(ItemToOrder itemToOrder) throws Exception {
        StockRequest stock = inventoryClient.getStock(itemToOrder.getSku()).getBody();
        if (stock.getQuantity() < itemToOrder.getQuantity()) throw new OrderException(Errors.STOCK_VALIDATION_NOT_ENOUGH.key(), Errors.STOCK_VALIDATION_NOT_ENOUGH.message());
    }

    // Circuit Breaker per ridurre lo stock della lista di prodotti
    @CircuitBreaker(name = "validation", fallbackMethod = "fallBackInventory")
    private void stockReduction(List<OrderItems> stocks){
        List<StockChange> l = new ArrayList<>();
        stocks.forEach(i -> {
            StockChange s = new StockChange();
            s.setQuantity(i.getQuantity());
            s.setSku(i.getSku());
            l.add(s);
        });
        inventoryClient.deductionStock(l);
    }
    // Circuit Breaker per aumentare lo stock della lista di prodotti
    @CircuitBreaker(name = "validation", fallbackMethod = "fallBackInventory")
    private void stockAddition(List<OrderItems> stocks){
        List<StockChange> l = new ArrayList<>();
        stocks.forEach(i -> {
            StockChange s = new StockChange();
            s.setQuantity(i.getQuantity());
            s.setSku(i.getSku());
            l.add(s);
        });
        inventoryClient.additionStock(l);
    }

    private OrderException fallBackProduct(Exception e) {
        return new OrderException(Errors.PRODUCT_SERVICE_DOWN.key(), Errors.PRODUCT_SERVICE_DOWN.message());
    }
    private OrderException fallBackInventory(Exception e) {
        return new OrderException(Errors.INVENTORY_SERVICE_DOWN.key(), Errors.INVENTORY_SERVICE_DOWN.message());
    }

    // Verifico l'esistenza e la disponibilità del prodotto
    private List<OrderItemDraft> orderValidation(List<ItemToOrder> itemToOrder) throws Exception {

        List<OrderItemDraft> orderDraftLs = new ArrayList<>();

        // Per ogni elemento della lista
        for (ItemToOrder toOrder : itemToOrder) {

            // OpendFeign + CircuitBreaker - verifico l'esistenza del prodotto
            ProductDto p = productValidation(toOrder);

            // OpenFeign + CircuitBreaker - verifico la disponibilità in stock
            stockValidation(toOrder);

            // Se esistente e presente in magazzino popolo una lista ordine di bozza che ritorno (OrderItemsDraft)
            OrderItemDraft orderItemDraft = new OrderItemDraft();
            orderItemDraft.setQuantity(toOrder.getQuantity());
            orderItemDraft.setSku(p.getSku());
            orderItemDraft.setPrice(p.getPrice());
            orderDraftLs.add(orderItemDraft);
        }

        return orderDraftLs;
    }

    private OrderItems convertOrderDraftToOrderItems(OrderItemDraft itemDraft){
        OrderItems orderItems = new OrderItems();
        orderItems.setUnitPrice(itemDraft.getPrice());
        orderItems.setQuantity(itemDraft.getQuantity());
        orderItems.setSku(itemDraft.getSku());
        return orderItems;
    }

    //! GET ALL ORDERS
    public Page<Order> getAllOrders(int pageSize, int page){
        Pageable pageable = PageRequest.of(page, pageSize);
        return orderRepository.findAllByDeletedFalse(pageable);
    }

    //! GET SINGLE ORDER
    public Order getSingleOrderById(UUID orderId, UUID userId, String usRole) {
        Order o = orderRepository.findByIdAndDeletedFalse(orderId).orElseThrow(()-> new OrderException(Errors.ORDER_NOT_FOUND.key(), Errors.ORDER_NOT_FOUND.message()));

        if (usRole.equals("ROLE_ADMIN") || (usRole.equals("ROLE_USER") && userId.equals(o.getUserId())) ) {

            return o;
        }
        throw new OrderException(Errors.USER_NOT_ALLOWED_FOR_ORDER.key(), Errors.USER_NOT_ALLOWED_FOR_ORDER.message());
    }

    //! CREATE ORDER
    @Transactional
    public void createOrder(List<ItemToOrder> itemList, UUID userId) throws Exception {

        // Controllo che userID non sia null
        if (userId == null) throw new OrderException(Errors.USER_NOT_ALLOWED_FOR_ORDER.key(), Errors.USER_NOT_ALLOWED_FOR_ORDER.message());

        // Valido i prodotti inseriti
        List<OrderItemDraft> orderDraft = orderValidation(itemList);

        // Creo ordine
        Order order = new Order();
        order.setOrderStatus(OrderStatus.BOZZA);
        order.setActive(true);

        // Assegno l'utente all'ordine
        order.setUserId(userId);

        // Mappo la lista degli items prodotto, in bozza, nella lista da salvare nell'ordine
        List<OrderItems> items = orderDraft.stream()
                    .map(itemDraft -> {
                        OrderItems item = convertOrderDraftToOrderItems(itemDraft);
                        // Assegno l'ordine di riferimento
                        item.setOrder(order);
                        return item;
                    })
                    .toList();

        // Calcolo il totale dell'ordine
        BigDecimal total = items.stream().map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assegno lista items ordinati e totale, salvo ordine
        order.setOrderItems(items);
        order.setTotal(total);
        orderRepository.save(order);
        // Riduzione dallo stock
        stockReduction(items);

        kafkaProducer.sendMessage(MESSAGE.formatted(order.getUserId(), order.getId()));
    }

    //! UPDATE
    public void updateOrder(UUID orderId, List<ItemToOrder> itemList) throws Exception {
        // Trovo l'ordine
        Order order = orderRepository.findOrderById(orderId).orElseThrow(()-> new OrderException(Errors.ORDER_NOT_FOUND.key(), Errors.ORDER_NOT_FOUND.message()));


        // Controllo se l'ordine è disattivato o eliminato
        if (order.getDeleted()) throw new OrderException(Errors.ORDER_ELIMINATED.key(), Errors.ORDER_ELIMINATED.message());
        if (!order.getActive()) throw new OrderException(Errors.ORDER_DEACTIVATED.key(), Errors.ORDER_DEACTIVATED.message());

        // Controllo se lo stato dell'ordine è compatibie con la modifica
        if (order.getOrderStatus() != OrderStatus.BOZZA) throw new OrderException(Errors.ORDER_STATUS_INCOMPATIBLE.key(), Errors.ORDER_STATUS_INCOMPATIBLE.message());

        // Ricarico la disponibilità per la lista prodotti precedente
        stockAddition(order.getOrderItems());

        // Valido i prodotti inseriti
        List<OrderItemDraft> orderDraft = orderValidation(itemList);

        // Mappo la lista degli items prodotto, in bozza, nella lista da salvare nell'ordine
        List<OrderItems> items = orderDraft.stream()
                .map( itemDraft -> {
                    OrderItems item = convertOrderDraftToOrderItems(itemDraft);
                    item.setOrder(order);
                    // Assengno l'ordine di riferimento a ogni item
                    return  item;
                })
                .toList();

        // Calcolo il totale dell'ordine
        BigDecimal total = items.stream().map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assegno lista items ordinati e totale, salvo ordine
        order.getOrderItems().clear();
        order.getOrderItems().addAll(items);
        order.setTotal(total);
        orderRepository.save(order);
        stockReduction(items);
    }

    //! CHANGE ORDER STATUS
    public void changeOrderStatus(UUID orderId, String status){
        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new OrderException(Errors.ORDER_NOT_FOUND.key(), Errors.ORDER_NOT_FOUND.message()));

        // Controllo se l'ordine è disattivato o eliminato
        if (order.getDeleted()) throw new OrderException(Errors.ORDER_ELIMINATED.key(), Errors.ORDER_ELIMINATED.message());
        if (!order.getActive()) throw new OrderException(Errors.ORDER_DEACTIVATED.key(), Errors.ORDER_DEACTIVATED.message());

        if(order.getOrderStatus().equals(OrderStatus.BOZZA) && status.equals("CONFERMATO")){
            order.setOrderStatus(OrderStatus.CONFERMATO);
        } else if (order.getOrderStatus().equals(OrderStatus.CONFERMATO) && status.equals("IN_LAVORAZIONE")){
            order.setOrderStatus(OrderStatus.IN_LAVORAZIONE);
        } else if(order.getOrderStatus().equals(OrderStatus.IN_LAVORAZIONE) && status.equals("EMESSO")){
            order.setOrderStatus(OrderStatus.EMESSO);
        } else {
            throw new OrderException(Errors.ORDER_STATUS_INCOMPATIBLE.key(), Errors.ORDER_STATUS_INCOMPATIBLE.message());
        }
        orderRepository.save(order);
    }

    //!DeActive
    // Disattivazione ordine
    public void deactiveOrder(UUID orderId) {
        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new OrderException(Errors.ORDER_NOT_FOUND.key(),Errors.ORDER_NOT_FOUND.message()));
        order.setActive(false);
        orderRepository.save(order);
    }

    //! ReActive
    // Riattivazione ordine
    public void reactivateOrder(UUID orderId) {

        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new OrderException(Errors.ORDER_NOT_FOUND.key(), Errors.ORDER_NOT_FOUND.message()));
        if (order.getDeleted()) throw new OrderException(Errors.ORDER_ELIMINATED.key(), Errors.ORDER_ELIMINATED.message());
        if (!order.getActive()) throw new OrderException(Errors.ORDER_DEACTIVATED.key(), Errors.ORDER_DEACTIVATED.message());
        order.setActive(true);
        orderRepository.save(order);
    }

    //! DELETE
    // Soft delete
    public void deleteOrder(UUID orderId) {
        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new OrderException(Errors.ORDER_NOT_FOUND.key(), Errors.ORDER_NOT_FOUND.message()));

        // Ricarico la disponibilità per la lista prodotti precedente
        stockAddition(order.getOrderItems());
        order.setDeleted(true);
        order.setActive(false);
        orderRepository.save(order);
    }


}
