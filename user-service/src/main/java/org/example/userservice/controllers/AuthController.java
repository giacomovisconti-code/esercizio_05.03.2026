package org.example.userservice.controllers;

import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.entities.User;
import org.example.userservice.jwt.JwtUtils;
import org.example.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){
        Optional<User> uOpt = userRepository.findUserByUsername(loginRequest.getUsername());

        // Controllo se esiste l'utente
        if (uOpt.isEmpty()){
            return ResponseEntity.badRequest().body("Utente non registrato!");
        }

        User user = uOpt.get();

        // Controllo s le password tra login request e utente salvato coincidono
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            return ResponseEntity.badRequest().body("Password errata!");
        }

        // Se coincidono genero il token
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().toString());

        return ResponseEntity.ok( new LoginResponse(user.getUsername(), user.getRole(), token));
    }
}
