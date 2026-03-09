package org.example.userservice.service;

import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.entities.User;
import org.example.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {

    // Importo il Password encoder
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserRepository userRepository;

    public void register(RegisterRequest registerRequest) throws Exception {

            if (userRepository.existsByUsername(registerRequest.getUsername())){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username già in uso");
            }

            User user = new User();
            user.setRole(registerRequest.getRole());
            user.setUsername(registerRequest.getUsername());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

            userRepository.save(user);
    }

    public UserResponse getUser(LoginRequest userLogin){
        Optional<User> userOpt = userRepository.findUserByUsername(userLogin.getUsername());

        if (userOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Non esiste uno user con questo nome");
        }

        UserResponse userResponse = new UserResponse(
                userOpt.get().getId(),
                userOpt.get().getUsername(),
                userOpt.get().getRole(),
                userOpt.get().getCreated_at()
        );

        return  userResponse;

    }
}
