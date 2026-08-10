package org.example.petinside.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

// 자격증명은 SDK 기본 체인을 그대로 사용: 로컬은 AWS_ACCESS_KEY_ID/SECRET 환경변수나 ~/.aws/credentials,
// EC2는 인스턴스에 붙은 IAM Role을 우선 사용 (키를 코드/서버에 따로 저장할 필요 없음, 권장 방식)
@Configuration
public class S3Config {

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
