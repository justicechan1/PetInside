package org.example.petinside.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

// 업로드된 이미지를 /images/** 로 정적 서빙. app.upload.dir(기본 ./uploads)의 실제 파일시스템 경로를 가리키므로
// jar 내부 리소스가 아니라 서버 디스크에 저장된 파일을 그대로 서빙한다.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/images/**")
                .addResourceLocations(location);
    }
}
