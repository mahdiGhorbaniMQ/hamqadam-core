package ir.hamqadam.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; // Optional: for default Spring MVC mapping

// Optional: If you want Spring MVC to map this to 403 Forbidden by default
// when not caught by a @ControllerAdvice. It's generally better to handle
// it in GlobalExceptionHandler for a consistent response format.
// @ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedException extends RuntimeException {

    /**
     * Constructs a new unauthorized exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public UnauthorizedException(String message) {
        super(message);
    }

    /**
     * Constructs a new unauthorized exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause (which is saved for later retrieval by the getCause() method).
     */
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}