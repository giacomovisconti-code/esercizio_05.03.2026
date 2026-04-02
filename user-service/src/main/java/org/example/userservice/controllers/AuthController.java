package org.example.userservice.controllers;

import jakarta.validation.Valid;
import org.apache.tomcat.websocket.AuthenticationException;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) throws AuthenticationException {
        return ResponseEntity.ok(authenticationService.login(loginRequest));
    }
}
