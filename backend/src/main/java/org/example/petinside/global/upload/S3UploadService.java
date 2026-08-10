package org.example.petinside.global.upload;

import org.example.petinside.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class S3UploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.presign-expiration-seconds}")
    private long presignExpirationSeconds;

    public S3UploadService(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    // 확장자만 클라이언트 입력으로 받고, 실제 객체 키는 서버가 UUID로 새로 생성 - 원본 파일명은 저장에 쓰지 않음
    public PresignedUrlResponse createPresignedUrl(String extension) {
        String normalized = normalizeExtension(extension);
        String key = "images/" + UUID.randomUUID() + "." + normalized;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignExpirationSeconds))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        String imageUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
        return new PresignedUrlResponse(presigned.url().toString(), imageUrl);
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new CustomException(400, "확장자는 필수입니다.");
        }
        String normalized = extension.toLowerCase(Locale.ROOT).replace(".", "");
        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new CustomException(400, "지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
        }
        return normalized;
    }
}
