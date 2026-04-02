package org.example.userservice.service;

import jakarta.transaction.Transactional;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.exceptions.Errors;
import org.example.userservice.exceptions.UserExcpetion;
import org.example.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UserService {

    // Importo il Password encoder
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserRepository userRepository;

    //? UTILS
    public UserResponse convertEntityToDto(User user){
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreated_at()
        );
    }

    public void register(RegisterRequest registerRequest) {

            if (userRepository.existsByUsername(registerRequest.getUsername())){
                throw new UserExcpetion(Errors.USERNAME_ALREADY_REGISTERED.key(), Errors.USERNAME_ALREADY_REGISTERED.message());
            }

            User user = new User();
            user.setRole(Role.ROLE_USER);
            user.setUsername(registerRequest.getUsername());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

            userRepository.save(user);
    }
    @Transactional
    public void giveAdminPermission(UUID userId) {
        User u = userRepository.findUserById(userId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

        u.setRole(Role.ROLE_ADMIN);
        userRepository.save(u);
    }

    public UserResponse getUser(LoginRequest userLogin){
        User u = userRepository.findUserByUsername(userLogin.getUsername()).orElseThrow(()-> new UserExcpetion(Errors.USER_NOT_FOUND.key(), Errors.USER_NOT_FOUND.message()));

        return convertEntityToDto(u);
    }

    public Page<UserResponse> getAllUsers(int pageSize, int page) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertEntityToDto);
    }
}
