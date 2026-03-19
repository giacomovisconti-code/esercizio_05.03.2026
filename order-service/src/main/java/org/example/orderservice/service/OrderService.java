package org.example.orderservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import org.example.orderservice.dto.order.OrderItemDraft;
import org.example.orderservice.dto.order.ItemToOrder;
import org.example.orderservice.dto.ProductDto;
import org.example.orderservice.dto.StockRequest;
import org.example.orderservice.entities.Order;
import org.example.orderservice.entities.OrderItems;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.kafka.KafkaProducer;
import org.example.orderservice.openfeign.InventoryClient;
import org.example.orderservice.openfeign.ProductClient;
import org.example.orderservice.repositories.OrderItemsRepository;
import org.example.orderservice.repositories.OrderRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final ModelMapper modelMapper = new ModelMapper();

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemsRepository orderItemsRepository;

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
    @CircuitBreaker(name = "validation")
    private ProductDto productValidation(ItemToOrder itemToOrder) throws Exception {
        ProductDto p = productClient.getProduct(itemToOrder.getSku()).getBody();
        if (p == null) throw new Exception("Prodotto non esistente! sku:" + itemToOrder.getSku());
        return p;
    }

    // Circuit Breaker per stock validation
    @CircuitBreaker(name = "validation")
    private void stockValidation(ItemToOrder itemToOrder) throws Exception {
        StockRequest stock = inventoryClient.getStock(itemToOrder.getSku()).getBody();
        if (stock.getQuantity() < itemToOrder.getQuantity()) throw new Exception("Quantità prodotto richiesta inferiore alla giacenza");
    }

    @CircuitBreaker(name = "validation")
    private void stockReduction(OrderItems itemToOrder){
        inventoryClient.deductionStock(itemToOrder.getSku(), itemToOrder.getQuantity());
    }

    @CircuitBreaker(name = "validation")
    private void stockAddition(OrderItems itemToReject){
        inventoryClient.additionStock(itemToReject.getSku(), itemToReject.getQuantity());
    }

    // Verifico l'esistenza e la disponibilità del prodotto
    private List<OrderItemDraft> orderValidation(List<ItemToOrder> itemToOrder) throws Exception {

        List<OrderItemDraft> orderDraftLs = new ArrayList<>();

        // Per ogni elemento della lista
        for (int i = 0; i < itemToOrder.size(); i++) {

            // OpendFeign + CircuitBreaker - verifico l'esistenza del prodotto
            ProductDto p = productValidation(itemToOrder.get(i));

            // OpenFeign + CircuitBreaker - verifico la disponibilità in stock
            stockValidation(itemToOrder.get(i));

            // Se esistente e presente in magazzino popolo una lista ordine di bozza che ritorno (OrderItemsDraft)
            OrderItemDraft orderItemDraft = new OrderItemDraft();
            orderItemDraft.setQuantity(itemToOrder.get(i).getQuantity());
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
    public List<Order> getAllOrders(){
        return orderRepository.findAllByDeletedFalse();
    }

    //! GET SINGLE ORDER
    public Order getSingleOrderById(UUID orderId, UUID userId, String usRole) throws Exception{
        Order o = orderRepository.findByIdAndDeletedFalse(orderId).orElseThrow(()->new RuntimeException("Nessun ordine trovato"));

        if (usRole.equals("ROLE_ADMIN") || (usRole.equals("ROLE_USER") && userId.equals(o.getUserId())) ) {

            return o;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non hai i permessi per visualizzare qusta risorsa");
    }

    //! CREATE ORDER
    @Transactional
    public void createOrder(List<ItemToOrder> itemList, UUID userId) throws Exception {

        // Controllo che userID non sia null
        if (userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UserId is null");

        // Valido i prodotti inseriti
        List<OrderItemDraft> orderDraft = orderValidation(itemList);

        // Creo ordine
        Order order = new Order();
        order.setOrderStatus(OrderStatus.STATUS_BOZZA);
        order.setActive(true);
        System.out.println("DEBUG: Stato ordine prima del save: " + order.getActive());

        // Assegno l'utente all'ordine
        order.setUserId(userId);

        // Mappo la lista degli items prodotto, in bozza, nella lista da salvare nell'ordine
        List<OrderItems> items = orderDraft.stream()
                .map( itemDraft -> {
                    OrderItems item = convertOrderDraftToOrderItems(itemDraft);
                    stockReduction(item);
                    // Assengno l'ordine di riferimento ad ogni item
                    item.setOrder(order);
                    return  item;
                        })
                .collect(Collectors.toList());

        // Calcolo il totale dell'ordine
        BigDecimal total = items.stream().map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assegno lista items ordinati e totale, salvo ordine
        order.setOrderItems(items);
        order.setTotal(total);
        orderRepository.save(order);
        System.out.println("DEBUG: Stato ordine prima del save: " + order.getActive());
        kafkaProducer.sendMessage(MESSAGE.formatted(order.getUserId(), order.getId()));

    }

    //! UPDATE
    public void updateOrder(UUID orderId, List<ItemToOrder> itemList) throws Exception {
        // Trovo l'ordine
        Order order = orderRepository.findOrderById(orderId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordine non trovato!"));


        // Controllo se l'ordine è disattivato o eliminato
        if (order.getDeleted()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordine eliminato, creane uno nuovo!");
        if (!order.getActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordine disattivato, riattivalo per applicare le modifiche!");

        // Controllo se lo stato dell'ordine è compatibie con la modifica
        if (order.getOrderStatus() != OrderStatus.STATUS_BOZZA) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ordine è già in fase avanzata");

        // Ricarico la disponibilità per la lista prodotti precedente
        order.getOrderItems().forEach(this::stockAddition);


        // Valido i prodotti inseriti
        List<OrderItemDraft> orderDraft = orderValidation(itemList);

        // Mappo la lista degli items prodotto, in bozza, nella lista da salvare nell'ordine
        List<OrderItems> items = orderDraft.stream()
                .map( itemDraft -> {
                    OrderItems item = convertOrderDraftToOrderItems(itemDraft);
                    stockReduction(item);
                    item.setOrder(order);
                    // Assengno l'ordine di riferimento ad ogni item
                    return  item;
                })
                .collect(Collectors.toList());

        // Calcolo il totale dell'ordine
        BigDecimal total = items.stream().map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assegno lista items ordinati e totale, salvo ordine
        order.getOrderItems().clear();
        order.getOrderItems().addAll(items);
        order.setTotal(total);
        orderRepository.save(order);
    }

    //! CHANGE ORDER STATUS
    public void changeOrderStatus(UUID orderId, String status){
        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordine non trovato!"));

        // Controllo se l'ordine è disattivato o eliminato
        if (order.getDeleted()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordine eliminato, creane uno nuovo!");
        if (!order.getActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordine disattivato, riattivalo per applicare le modifiche!");

        if(order.getOrderStatus().equals(OrderStatus.STATUS_BOZZA) && status.equals("STATUS_CONFERMATO")){
            order.setOrderStatus(OrderStatus.STATUS_CONFERMATO);
        } else if (order.getOrderStatus().equals(OrderStatus.STATUS_CONFERMATO) && status.equals("STATUS_IN_LAVORAZIONE")){
            order.setOrderStatus(OrderStatus.STATUS_IN_LAVORAZIONE);
        } else if(order.getOrderStatus().equals(OrderStatus.STATUS_IN_LAVORAZIONE) && status.equals("STATUS_EMESSO")){
            order.setOrderStatus(OrderStatus.STATUS_EMESSO);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stato ordine non compatibile con la modifica richiesta!");
        }
        orderRepository.save(order);
    }

    //!DeActive
    // Disattivazione ordine
    public void deactiveOrder(UUID orderId) throws Exception {
        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordine non trovato!"));
        order.setActive(false);
        orderRepository.save(order);
    }

    //! ReActive
    // Riattivazione ordine
    public void reactivateOrder(UUID orderId) throws Exception {

        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordine non trovato!"));
        if (order.getDeleted()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordine eliminato, non è possibile riattivarlo!");
        if (order.getActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordine già attivo!");
        order.setActive(true);
        orderRepository.save(order);
    }

    //! DELETE
    // Soft delete
    public void deleteOrder(UUID orderId) throws Exception {
        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordine non trovato!"));

        // Ricarico la disponibilità per la lista prodotti precedente
        order.getOrderItems().forEach(this::stockAddition);
        order.setDeleted(true);
        order.setActive(false);
        orderRepository.save(order);
    }


}
