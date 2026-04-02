package org.example.userservice.service;

import org.apache.tomcat.websocket.AuthenticationException;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.Errors;
import org.example.userservice.jwt.JwtUtils;
import org.example.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticationService {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest loginRequest) throws AuthenticationException {
        Optional<User> uOpt = userRepository.findUserByUsername(loginRequest.getUsername());

        // Controllo se esiste l'utente
        if (uOpt.isEmpty()){
            throw new AuthenticationException(Errors.USER_NOT_FOUND.key());
        }

        User user = uOpt.get();

        // Controllo s le password tra login request e utente salvato coincidono
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new AuthenticationException(Errors.WRONG_PASSWORD.key());

        }

        // Se coincidono genero il token
        String token = jwtUtils.generateToken(user.getId().toString(), user.getRole().toString());

        return new LoginResponse(user.getUsername(), user.getRole(), token);
    }
}
