package org.example.petinside;

import org.example.petinside.global.portone.PortOneProperties;
import org.example.petinside.global.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, PortOneProperties.class})
public class PetInsideApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetInsideApplication.class, args);
    }

}
