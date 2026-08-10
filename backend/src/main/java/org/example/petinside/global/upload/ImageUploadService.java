package org.example.petinside.global.upload;

import lombok.extern.slf4j.Slf4j;
import org.example.petinside.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// S3 presigned URL(S3UploadService) 대신 EC2 로컬 디스크에 직접 저장하는 버전.
// 파일이 이 서버를 거쳐가므로 presign 발급 단계 없이 업로드 API 한 번으로 끝남.
@Slf4j
@Service
public class ImageUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${app.upload.dir}")
    private String uploadDir;

    // 원본 파일명은 저장에 쓰지 않고 확장자만 검증 후 추출 - path traversal("../..") 방지
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(400, "업로드할 파일이 없습니다.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(400, "지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
        }

        String filename = UUID.randomUUID() + "." + extension;

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            Path target = dir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("이미지 저장 실패", e);
            throw new CustomException(500, "이미지 저장에 실패했습니다.");
        }

        return filename;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new CustomException(400, "파일 확장자를 확인할 수 없습니다.");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
