package org.example.userservice.dto;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.enums.Role;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username cannot blank")
    @Size(min = 3, max = 30, message = "Username cannot be shorter than 3 chars")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username not valid")
    private String username;

    @Length(min = 6, message = "Password cannot be shoter than 6 chars")
    @NotBlank(message = "Password field empty!")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Weak password: it must contain at least one uppercase letter, one lowercase letter, and one number"
    )
    private String password;


}
