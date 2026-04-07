package org.example.userservice.controllers;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/all")
    public ResponseEntity<Page<UserResponse>> getUsers(@Parameter(description = "Risultati per pagina", schema = @Schema(defaultValue = "9", minimum = "1"))
                                                           @RequestParam(name = "pageSize", required = false, defaultValue = "9") @Positive(message = "Deve essere >= 1") Integer pageSize,
                                                       @Parameter(description = "Indice della pagina corrente", schema = @Schema(defaultValue = "0", minimum = "0"))
                                                           @RequestParam(name = "page", required = false, defaultValue = "0") @Min(value = 0, message = "Indice di pagina deve essere >= 0") Integer page){
        return ResponseEntity.ok(userService.getAllUsers(pageSize, page));
    }

    @PostMapping("/register")
    public ResponseEntity<String> createUser(@Valid @RequestBody RegisterRequest registerRequest) throws Exception {

        userService.register(registerRequest);

        return ResponseEntity.ok("Utente creato con successo");

    }

    @PatchMapping("/giveAdmin/{userId}")
    public ResponseEntity<String> giveAdminPermission(@NotNull @PathVariable("userId") UUID userId){
        userService.giveAdminPermission(userId);
        return ResponseEntity.ok("L'utente ha ora i permessi di Admin");
    }


}
