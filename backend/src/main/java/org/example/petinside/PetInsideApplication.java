package org.example.petinside;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PetInsideApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetInsideApplication.class, args);
    }

}
