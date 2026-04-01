package com.example.guangzhouorder.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignUpRequest {

    @NotBlank(message = "Full name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).+$",
        message = "Password must contain at least one uppercase letter and one special character"
    )
    private String password;

    @NotBlank(message = "Please select an account type")
    @Pattern(regexp = "^(CUSTOMER|AFFILIATE)$", message = "Invalid account type selected")
    private String role = "CUSTOMER";
}
