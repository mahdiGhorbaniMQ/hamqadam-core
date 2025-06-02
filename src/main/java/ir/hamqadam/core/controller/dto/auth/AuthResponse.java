package ir.hamqadam.core.controller.dto.auth;

import ir.hamqadam.core.controller.dto.user.UserResponseDTO; // Renamed for clarity
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private UserResponseDTO user; // Contains user details

    public AuthResponse(String accessToken, UserResponseDTO user) {
        this.accessToken = accessToken;
        this.user = user;
    }
}