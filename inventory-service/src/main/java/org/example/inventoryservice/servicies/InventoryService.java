package org.example.inventoryservice.servicies;

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
    private void StockChangeValidation(StockChange stock) throws  Exception {

        // Verifico se il productId è presente in Inventory
        if (inventoryRepository.findByProductId(stock.getProductId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Non esiste un prodotto con qusto ProductID");
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
    public StockRequest getStockByProductId(UUID productId){

        // Cerco tramite il product id la giacenza relativa
        Optional<Inventory> stockOpt = inventoryRepository.findByProductId(productId);

        // Verifico se esiste
        if (stockOpt.isEmpty()){

            // Se non esiste lancio un' Eccezione
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Non esiste un prodotto con qusto ProductID");
        }

        // Se esiste ritorno convertendolo in Dto
        return convertInventoryToStockRequest(stockOpt.get());
    }

    //? CREAZIONE GIACENZA
    // Creazione automatica nel momento dell'aggiunta del prodotto
    public void initializeStock(UUID productId) throws Exception {

        // Controllo se il prodotto è già presente in inventario
        if (inventoryRepository.findByProductId(productId).isPresent()){
            // Se presente lancio una eccezione di conflitto
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prodotto già inventariato");
        }

        // Se non è presente lo inizializzo a zero, con productId inserito
        Inventory initialized = new Inventory();
        initialized.setProductId(productId);
        initialized.setQuantity(0L);
        inventoryRepository.save(initialized);

    }

    //? MODIFICA GIACENZA
    // Modifica manuale
    public void modifyStock(StockChange stockChange) throws Exception {

        // Valido il Dto in entrata
        StockChangeValidation(stockChange);

        Inventory stock = inventoryRepository.findByProductId(stockChange.getProductId()).get();

        // Modifico la quantità
        stock.setQuantity(stockChange.getQuantity());

        // Salvo lo stock
        inventoryRepository.save(stock);
    }

    // Aggiunta (il numero inserito si somma alla giacenza corrente)
    @Transactional
    public void addStock(UUID productId, Long quantity){
        Optional<Inventory> invOpt = inventoryRepository.findByProductId(productId);

        // Verifico se è presente la giacenza per un prodotto con quel productId
        if (invOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Inventory inv = invOpt.get();

        if (quantity <= 0){
            throw  new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inserire una quantità positiva.");
        }

        inv.setQuantity(inv.getQuantity() + quantity);
        inventoryRepository.save(inv);

    }

    // Deduzione (il numero inserito si sottrae alla giacenza corrente)
    @Transactional
    public void deductStock(UUID productId, Long quantity){
        Optional<Inventory> invOpt = inventoryRepository.findByProductId(productId);

        // Verifico se è presente la giacenza per un prodotto con quel productId
        if (invOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Inventory inv = invOpt.get();

        if (quantity > inv.getQuantity()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La quantità richiesta è inferiore alla giacenza." );
        }

        inv.setQuantity(inv.getQuantity() - quantity);
        inventoryRepository.save(inv);

    }

    //? ELIMINAZIONE GIACENZA
    // Eliminazione automatica con l'eliminazione del prootto
    @Transactional
    public void deleteStock(UUID productId){

        // Verifico l'esistenza del prodotto con il productId in entrata
        if (inventoryRepository.findByProductId(productId).isEmpty()){
            throw  new ResponseStatusException(HttpStatus.NOT_FOUND, "Prodotto non trovato");
        }

        // Se esistente elimino la giacenza
        inventoryRepository.deleteByProductId(productId);
    }
}
