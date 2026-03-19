package org.example.userservice.dto;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
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
    @Length(min = 3, message = "Username cannot be shoter than 3 chars")
    private String username;

    @Length(min = 6, message = "Password cannot be shoter than 6 chars")
    private String password;


}
