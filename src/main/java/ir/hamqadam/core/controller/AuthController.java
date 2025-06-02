package ir.hamqadam.core.controller;

import ir.hamqadam.core.controller.dto.auth.*;
import ir.hamqadam.core.controller.dto.common.MessageResponse;
import ir.hamqadam.core.controller.dto.user.UserResponseDTO; // Assuming a UserResponse DTO
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.security.jwt.JwtTokenProvider;
import ir.hamqadam.core.service.UserService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
// Assuming ModelMapper or similar for DTO conversion
// import org.modelmapper.ModelMapper;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    // private final ModelMapper modelMapper; // If using ModelMapper

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          JwtTokenProvider jwtTokenProvider
            /*, ModelMapper modelMapper */) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        // this.modelMapper = modelMapper;
    }

    @PostMapping("/register/email")
    public ResponseEntity<?> registerUserByEmail(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        User registeredUser = userService.registerNewUserByEmail(
                registrationRequest.getFullName(), // Assuming UserRegistrationRequest has getFullName()
                registrationRequest.getEmail(),
                registrationRequest.getPassword()
        );
        // For Phase 1, maybe don't log in immediately after registration, require verification.
        // Or return a message: "Verification email sent."
        // UserResponse userResponse = modelMapper.map(registeredUser, UserResponse.class); // Example mapping
        return ResponseEntity.ok(new MessageResponse("User registered successfully! Please check your email to verify."));
    }

    @PostMapping("/login/email")
    public ResponseEntity<AuthResponse> loginUserByEmail(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findUserByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication")); // Should not happen

        // UserResponse userResponse = modelMapper.map(user, UserResponse.class); // Example mapping
        UserResponseDTO userResponse = convertUserToUserResponse(user); // Placeholder for mapping

        return ResponseEntity.ok(new AuthResponse(jwt, userResponse));
    }

    @PostMapping("/telegram")
    public ResponseEntity<AuthResponse> loginOrRegisterTelegramUser(@RequestBody TelegramAuthRequest telegramAuthRequest) {
        // In a real scenario, telegramAuthRequest would contain validated data from Telegram
        // (e.g., data received from Telegram Login Widget or bot authentication).
        // For simplicity, assume it contains necessary fields directly.
        User user = userService.registerOrLoginTelegramUser(
                telegramAuthRequest.getTelegramId(),
                telegramAuthRequest.getTelegramUsername(),
                telegramAuthRequest.getFullName()
        );

        // Create UserDetails for token generation
        // Note: UserDetailsService (UserServiceImpl) loads by email by default, adapt if needed for telegram ID
        // Or generate token directly if JwtTokenProvider supports User object or UserDetails from User object
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getTelegramId()) // Or a unique identifier used for UserDetails
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "") // Dummy password if not applicable
                .authorities("ROLE_USER") // Placeholder, derive from user object
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(user.getAccountStatus() != User.AccountStatus.ACTIVE)
                .build();

        String jwt = jwtTokenProvider.generateToken(userDetails);
        // UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        UserResponseDTO userResponse = convertUserToUserResponse(user); // Placeholder for mapping

        return ResponseEntity.ok(new AuthResponse(jwt, userResponse));
    }

    // Placeholder DTO for Telegram Auth
    // static class TelegramAuthRequest {
    //     public String telegramId;
    //     public String telegramUsername;
    //     public Map<String, String> fullName;
    //     // Getters & Setters
    // }


    // Example for Password Reset (simplified, actual implementation needs token storage/validation)
    @PostMapping("/password-reset/request")
    public ResponseEntity<MessageResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest resetRequest) {
        // userService.generatePasswordResetToken(resetRequest.getEmail());
        return ResponseEntity.ok(new MessageResponse("If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<MessageResponse> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest confirmRequest) {
        // userService.resetPassword(confirmRequest.getToken(), confirmRequest.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully."));
    }

    // Placeholder for mapping User to UserResponse DTO
    private UserResponseDTO convertUserToUserResponse(User user) {
        if (user == null) return null;
        // Manual mapping or use a library like ModelMapper/MapStruct
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setTelegramId(user.getTelegramId());
        dto.setTelegramUsername(user.getTelegramUsername());
        dto.setProfilePictures(user.getProfilePictures());
        dto.setBio(user.getBio());
        dto.setAccountStatus(user.getAccountStatus());
        // ... map other necessary fields
        return dto;
    }

    // Define DTOs like UserRegistrationRequest, LoginRequest, AuthResponse, UserResponse separately
    // For brevity, an example AuthResponse structure is:
    // public record AuthResponse(String accessToken, String tokenType, UserResponse user) {
    //     public AuthResponse(String accessToken, UserResponse user) {
    //         this(accessToken, "Bearer", user);
    //     }
    // }
    // And UserResponse:
    // @Data public class UserResponse { private String userId; private Map<String, String> fullName; private String email; /* ... other fields ... */ }
}

// Define DTOs used by AuthController here or in a separate dto.auth package
// Example:
// package ir.hamqadam.core.controller.dto.auth;
// public record LoginRequest(String email, String password) {}
// public record UserRegistrationRequest(Map<String, String> fullName, String email, String password) {}
// public record AuthResponse(String accessToken, String tokenType, UserResponse user) { /* ... */ }
// public record PasswordResetRequest(String email) {}
// public record PasswordResetConfirmRequest(String token, String newPassword) {}

// package ir.hamqadam.core.controller.dto.user;
// @Data public class UserResponse { /* ... fields ... */ }

// package ir.hamqadam.core.controller.dto.common;
// public record MessageResponse(String message) {}