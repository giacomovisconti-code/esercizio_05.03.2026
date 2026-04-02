package org.example.inventoryservice.services;

import jakarta.transaction.Transactional;
import org.example.inventoryservice.dto.StockChange;
import org.example.inventoryservice.dto.StockRequest;
import org.example.inventoryservice.entities.Inventory;
import org.example.inventoryservice.exceptions.InventoryException;
import org.example.inventoryservice.exceptions.handler.Errors;
import org.example.inventoryservice.repositories.InventoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CacheManager cacheManager;

    private final ModelMapper modelMapper = new ModelMapper();

    //* UTILS
    // Validation
    private void StockChangeValidation(StockChange stock) {

        // Verifico se il productId è presente in Inventory
        if (inventoryRepository.findBySku(stock.getSku()).isEmpty()) {
            throw new InventoryException(Errors.STOCK_NOT_FOUND.key(), Errors.STOCK_NOT_FOUND.message());
        }

        // Controllo se la quantità inserità dall'utente non è minore di Zero
        if (stock.getQuantity() < 0) {
            throw new InventoryException(Errors.INVALID_STOCK_QTY.key(), Errors.INVALID_STOCK_QTY.message());
        }
    }

    // Inventory --> StockRequest
    private StockRequest convertInventoryToStockRequest(Inventory inventory){
        return modelMapper.map(inventory, StockRequest.class);
    }

    //? LISTA DI GIACENZE
    public Page<StockRequest> getAllStock(int pageSize, int page){
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Inventory> list = inventoryRepository.findAll(pageable);

        return list.map(this::convertInventoryToStockRequest);
    }

    //? SINGOLA GIACENZA
    @Cacheable(value = "stock", key = "#sku")
    public StockRequest getStockByProductId(UUID sku){

        // Cerco tramite il product id la giacenza relativa
        Optional<Inventory> stockOpt = inventoryRepository.findBySku(sku);

        // Verifico se esiste
        if (stockOpt.isEmpty()){

            // Se non esiste lancio un' Eccezione
            throw new InventoryException(Errors.STOCK_NOT_FOUND.key(), Errors.STOCK_NOT_FOUND.message());
        }

        // Se esiste ritorno convertendolo in Dto
        return convertInventoryToStockRequest(stockOpt.get());
    }

    //? CREAZIONE GIACENZA
    @CacheEvict(value = "stock", key = "#sku")
    // Creazione automatica nel momento dell'aggiunta del prodotto
    public void initializeStock(UUID sku) {

        // Controllo se il prodotto è già presente in inventario
        if (inventoryRepository.findBySku(sku).isPresent()){
            // Se presente lancio una eccezione di conflitto
            throw new InventoryException(Errors.STOCK_ALREADY_REGISTERED.key(), Errors.STOCK_ALREADY_REGISTERED.message());
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

        Inventory stock = inventoryRepository.findBySku(stockChange.getSku()).orElseThrow(() -> new InventoryException(Errors.STOCK_NOT_FOUND.key(), Errors.STOCK_NOT_FOUND.message()));

        // Modifico la quantità
        stock.setQuantity(stockChange.getQuantity());

        // Salvo lo stock
        inventoryRepository.save(stock);
        // elimino il dato dalla cache
        Objects.requireNonNull(cacheManager.getCache("stock")).evictIfPresent(stockChange.getSku());
    }

    // Aggiunta (il numero inserito si somma alla giacenza corrente)
    @Transactional
    public void addStock(List<StockChange> items){
        if (items.isEmpty()) throw new InventoryException(Errors.STOCK_NOT_FOUND.key(), Errors.STOCK_NOT_FOUND.message());
        items.forEach( i -> {

            Inventory inv = inventoryRepository.findBySku(i.getSku()).orElseThrow(()-> new InventoryException(Errors.STOCK_NOT_FOUND.key(), Errors.STOCK_NOT_FOUND.message()));

            // Verifico che la quantità da dedurre non sia minore o uguale a zero o che non sia nulla
            if (i.getQuantity() == null || i.getQuantity() <= 0) throw new InventoryException(Errors.INVALID_STOCK_QTY.key(), Errors.INVALID_STOCK_QTY.message());

            inv.setQuantity(inv.getQuantity() + i.getQuantity());
            inventoryRepository.save(inv);

            // elimino il dato dalla cache
            Objects.requireNonNull(cacheManager.getCache("stock")).evictIfPresent(i.getSku());
        });


    }

    // Deduzione (il numero inserito si sottrae alla giacenza corrente)
    @Transactional
    public void deductStock(List<StockChange> items){
        if (items.isEmpty()) throw new InventoryException(Errors.NO_ITEMS.key(), Errors.NO_ITEMS.message());
        items.forEach( i -> {

            Inventory inv = inventoryRepository.findBySku(i.getSku()).orElseThrow(()-> new InventoryException(Errors.STOCK_NOT_FOUND.key(), Errors.STOCK_NOT_FOUND.message()));

            // Verifico che la quantità da dedurre non sia minore o uguale a zero o che non sia nulla
            if (i.getQuantity() == null || i.getQuantity() <= 0) throw new InventoryException(Errors.INVALID_STOCK_QTY.key(), Errors.INVALID_STOCK_QTY.message());

            // Verifico che la quantità richiesta non sia maggiore della giacenza
            if (i.getQuantity() > inv.getQuantity()){
                throw new InventoryException(Errors.NOT_ENOUGH_STOCK.key(), Errors.NOT_ENOUGH_STOCK.message());
            }

            inv.setQuantity(inv.getQuantity() - i.getQuantity());
            inventoryRepository.save(inv);

            // elimino il dato dalla cache
            Objects.requireNonNull(cacheManager.getCache("stock")).evictIfPresent(i.getSku());
        });

    }

    //? ELIMINAZIONE GIACENZA
    // Eliminazione automatica con l'eliminazione del prodotto
    @Transactional
    @CacheEvict(value = "stock", key = "#sku")
    public void deleteStock(UUID sku){

        // Verifico l'esistenza del prodotto con il productId in entrata
        if (inventoryRepository.findBySku(sku).isEmpty()){
            throw new InventoryException(Errors.STOCK_NOT_FOUND.key(), Errors.STOCK_NOT_FOUND.message());
        }

        // Se esistente elimino la giacenza
        inventoryRepository.deleteBySku(sku);
    }
}
