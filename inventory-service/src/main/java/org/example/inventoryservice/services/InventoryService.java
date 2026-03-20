package org.example.inventoryservice.services;

import jakarta.transaction.Transactional;
import org.example.inventoryservice.dto.StockChange;
import org.example.inventoryservice.dto.StockRequest;
import org.example.inventoryservice.entities.Inventory;
import org.example.inventoryservice.repositories.InventoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    //* UTILS
    // Validation
    private void StockChangeValidation(StockChange stock) {

        // Verifico se il productId è presente in Inventory
        if (inventoryRepository.findBySku(stock.getSku()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Non esiste un prodotto con questo ProductID");
        }

        // Controllo se la quantità inserità dall'utente non è minore di Zero
        if (stock.getQuantity() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantità deve essere maggiore di 0");
        }
    }

    // Inventory --> StockRequest
    private StockRequest convertInventoryToStockRequest(Inventory inventory){
        return modelMapper.map(inventory, StockRequest.class);
    }

    //? LISTA DI GIACENZE
    public List<StockRequest> getAllStock(){
        List<Inventory> list = inventoryRepository.findAll();

        return list.stream()
                .map(this::convertInventoryToStockRequest)
                .collect(Collectors.toList());
    }

    //? SINGOLA GIACENZA
    public StockRequest getStockByProductId(UUID sku){

        // Cerco tramite il product id la giacenza relativa
        Optional<Inventory> stockOpt = inventoryRepository.findBySku(sku);

        // Verifico se esiste
        if (stockOpt.isEmpty()){

            // Se non esiste lancio un' Eccezione
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Non esiste un prodotto con questo ProductID");
        }

        // Se esiste ritorno convertendolo in Dto
        return convertInventoryToStockRequest(stockOpt.get());
    }

    //? CREAZIONE GIACENZA
    // Creazione automatica nel momento dell'aggiunta del prodotto
    public void initializeStock(UUID sku) {

        // Controllo se il prodotto è già presente in inventario
        if (inventoryRepository.findBySku(sku).isPresent()){
            // Se presente lancio una eccezione di conflitto
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prodotto già inventariato");
        }

        // Se non è presente lo inizializzo a zero, con productId inserito
        Inventory initialized = new Inventory();
        initialized.setSku(sku);
        initialized.setQuantity(0L);
        inventoryRepository.save(initialized);

    }

    //? MODIFICA GIACENZA
    // Modifica manuale
    public void modifyStock(StockChange stockChange) {

        // Valido il Dto in entrata
        StockChangeValidation(stockChange);

        Inventory stock = inventoryRepository.findBySku(stockChange.getSku()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST , "Stock non presente"));

        // Modifico la quantità
        stock.setQuantity(stockChange.getQuantity());

        // Salvo lo stock
        inventoryRepository.save(stock);
    }

    // Aggiunta (il numero inserito si somma alla giacenza corrente)
    @Transactional
    public void addStock(List<StockChange> items){
        if (items.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nessun prodotto inserito");
        items.forEach( i -> {

            Inventory inv = inventoryRepository.findBySku(i.getSku()).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

            // Verifico che la quantità da dedurre non sia minore o uguale a zero o che non sia nulla
            if (i.getQuantity() == null || i.getQuantity() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantità da dedurre è inferiore o uguale a zero");

            inv.setQuantity(inv.getQuantity() + i.getQuantity());
            inventoryRepository.save(inv);
        });


    }

    // Deduzione (il numero inserito si sottrae alla giacenza corrente)
    @Transactional
    public void deductStock(List<StockChange> items){
        if (items.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nessun prodotto inserito");
        items.forEach( i -> {

            Inventory inv = inventoryRepository.findBySku(i.getSku()).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

            // Verifico che la quantità da dedurre non sia minore o uguale a zero o che non sia nulla
            if (i.getQuantity() == null || i.getQuantity() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantità da dedurre è inferiore o uguale a zero");

            // Verifico che la quantità richiesta non sia maggiore della giacenza
            if (i.getQuantity() > inv.getQuantity()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La quantità richiesta è inferiore alla giacenza." );
            }

            inv.setQuantity(inv.getQuantity() - i.getQuantity());
            inventoryRepository.save(inv);
        });

    }

    //? ELIMINAZIONE GIACENZA
    // Eliminazione automatica con l'eliminazione del prodotto
    @Transactional
    public void deleteStock(UUID sku){

        // Verifico l'esistenza del prodotto con il productId in entrata
        if (inventoryRepository.findBySku(sku).isEmpty()){
            throw  new ResponseStatusException(HttpStatus.NOT_FOUND, "Prodotto non trovato");
        }

        // Se esistente elimino la giacenza
        inventoryRepository.deleteBySku(sku);
    }
}
