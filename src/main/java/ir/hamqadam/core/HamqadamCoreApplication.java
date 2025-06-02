package ir.hamqadam.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class HamqadamCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(HamqadamCoreApplication.class, args);
    }

}
