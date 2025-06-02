package ir.hamqadam.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy // This enables support for handling components marked with @Aspect
public class AopConfig {
    // This class can be empty if @EnableAspectJAutoProxy is all you need
    // and your aspects are component-scanned.
    // You might define @Bean methods here if your aspects need to be
    // configured or created in a specific way.
}