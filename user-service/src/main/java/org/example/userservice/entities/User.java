package org.example.userservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.Role;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @NotBlank(message = "Username cannot blank")
    @Length(min = 3, message = "Username cannot be shoter than 3 chars")
    private String username;

    @Column(nullable = false)
    private String password;

    @NotBlank(message = "role cannot be blank")
    private Role role;
}
