package org.example.userservice.repositories;

import org.example.userservice.entities.User;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Page<User> findAll(@NonNull Pageable pageable);

    Optional<User> findUserByUsername(String username);
    Optional<User> findUserById(UUID userId);

    Boolean existsByUsername(String username);
}
