package ir.hamqadam.core.config;

// import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.data.mongodb.config.EnableMongoAuditing;
// import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
// import java.util.ArrayList;
// import java.util.List;

@Configuration
// @EnableMongoAuditing // Uncomment if you want to use @CreatedDate, @LastModifiedDate etc.
public class MongoConfig {

    // Example: Custom MongoDB Converters
    // @Bean
    // public MongoCustomConversions customConversions() {
    //     List<Converter<?, ?>> converters = new ArrayList<>();
    //     // Add your custom converters here if needed
    //     // e.g., converters.add(new YourCustomReadConverter());
    //     // converters.add(new YourCustomWriteConverter());
    //     return new MongoCustomConversions(converters);
    // }

    // Further MongoDB specific configurations can be added here
    // e.g., configuring MongoTemplate, GridFsTemplate, transaction manager, etc.
    // For most Phase 1 scenarios, auto-configuration is sufficient.
}