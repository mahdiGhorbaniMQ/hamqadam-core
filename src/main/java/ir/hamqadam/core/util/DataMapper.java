package ir.hamqadam.core.util;

// import com.hamqadam.controller.dto.response.UserResponse;
// import com.hamqadam.model.User;
// import org.springframework.stereotype.Component;

// @Component // If it holds state or needs to be injected
public class DataMapper {

    // Example: Manual mapping method (you'd have many of these)
    // public static UserResponse toUserResponse(User user) {
    //     if (user == null) {
    //         return null;
    //     }
    //     UserResponse userResponse = new UserResponse();
    //     userResponse.setUserId(user.getUserId());
    //     userResponse.setFullName(user.getFullName()); // Assuming direct mapping for i18n string
    //     userResponse.setEmail(user.getEmail());
    //     // ... map other fields
    //     return userResponse;
    // }

    // For Phase 1, consider putting static mapping methods directly in DTOs (e.g., UserResponse.fromEntity(User user))
    // or using a dedicated mapping library like MapStruct for more complex scenarios.
    // This class can be expanded with common, generic mapping utilities if needed.
}