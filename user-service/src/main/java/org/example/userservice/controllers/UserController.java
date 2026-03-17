package org.example.userservice.controllers;


import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/all")
    public ResponseEntity<List<User>> getUsers(){
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/register")
    public ResponseEntity<String> createUser(@RequestBody RegisterRequest registerRequest) throws Exception {

        userService.register(registerRequest);

        return ResponseEntity.ok("Utente creato con successo");

    }

//    @PostMapping("/create-user")
//    public ResponseEntity<String> createUser(){
//        User user = new User();
//        user.setRole(Role.ROLE_USER);
//        user.setUsername("userTest");
//        user.setPassword(passwordEncoder.encode("user1234"));
//        userRepository.save(user);
//        return ResponseEntity.ok("Utente creato con successo");
//    }
//
//    @PostMapping("/create-admin")
//    public ResponseEntity<String> createAdmin(){
//        User user = new User();
//        user.setRole(Role.ROLE_USER);
//        user.setUsername("userTest");
//        user.setPassword(passwordEncoder.encode("user1234"));
//        userRepository.save(user);
//        return ResponseEntity.ok("Utente creato con successo");
//    }

}
