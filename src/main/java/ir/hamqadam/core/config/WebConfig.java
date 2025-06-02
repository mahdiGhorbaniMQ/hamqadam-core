package ir.hamqadam.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // If you handle CORS primarily in SecurityConfig, this might be simpler or not needed.
    // However, WebMvcConfigurer provides another way to configure global CORS.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Configure path pattern
                .allowedOriginPatterns("*") // Or specific origins: "http://localhost:3000"
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Max age of the pre-flight request
    }

    // You can also configure interceptors, view resolvers, argument resolvers, etc. here
    // For example:
    // @Override
    // public void addInterceptors(InterceptorRegistry registry) {
    //     registry.addInterceptor(new LoggingInterceptor()); // Assuming LoggingInterceptor exists
    // }
}