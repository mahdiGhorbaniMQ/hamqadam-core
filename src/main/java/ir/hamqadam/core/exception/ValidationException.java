package ir.hamqadam.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; // If you want Spring to automatically map it

import java.util.Collections;
import java.util.Map;

// You can choose to annotate it with @ResponseStatus if you want Spring MVC
// to automatically map this exception to an HTTP status code when it's thrown
// from a controller and not caught by a @ControllerAdvice.
// However, it's often better to handle it in GlobalExceptionHandler for a consistent response format.
// @ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    /**
     * Constructs a new validation exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public ValidationException(String message) {
        super(message);
        this.errors = Collections.emptyMap();
    }

    /**
     * Constructs a new validation exception with the specified detail message and a map of field-specific errors.
     *
     * @param message the detail message.
     * @param errors  a map where keys are field names and values are error messages.
     */
    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = (errors != null) ? Collections.unmodifiableMap(errors) : Collections.emptyMap();
    }

    /**
     * Constructs a new validation exception with the specified detail message, a map of field-specific errors, and cause.
     *
     * @param message the detail message.
     * @param errors  a map where keys are field names and values are error messages.
     * @param cause the cause.
     */
    public ValidationException(String message, Map<String, String> errors, Throwable cause) {
        super(message, cause);
        this.errors = (errors != null) ? Collections.unmodifiableMap(errors) : Collections.emptyMap();
    }


    /**
     * Returns an unmodifiable map of field-specific errors.
     * The keys are field names and the values are error messages.
     * Returns an empty map if no field-specific errors were provided.
     *
     * @return a map of field errors.
     */
    public Map<String, String> getErrors() {
        return errors;
    }
}