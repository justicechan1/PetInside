package org.example.petinside;

import org.example.petinside.global.portone.PortOneProperties;
import org.example.petinside.global.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, PortOneProperties.class})
@EnableScheduling
public class PetInsideApplication {

    // 배포 서버(EC2) OS/JVM 기본 타임존이 UTC라서, JDBC URL의 serverTimezone=Asia/Seoul과 어긋나
    // DATETIME 컬럼을 읽을 때마다 9시간이 밀리는 문제가 있었다. JVM 기본 타임존을 명시적으로
    // Asia/Seoul로 고정해서 이 어긋남 자체를 없앤다.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(PetInsideApplication.class, args);
    }

}
