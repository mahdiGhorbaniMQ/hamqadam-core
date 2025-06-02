package ir.hamqadam.core.security;

public final class SecurityConstants {

    public static final String AUTH_HEADER_STRING = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    // public static final long EXPIRATION_TIME = 864_000_000; // 10 days in ms - better to configure in properties
    // public static final String SECRET = "YourJWTSecretKey"; // Better to configure in properties

    private SecurityConstants() {
        // Prevents instantiation
    }
}