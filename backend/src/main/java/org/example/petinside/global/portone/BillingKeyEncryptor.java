package org.example.petinside.global.portone;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

// 빌링키는 평문 저장/로그 노출이 금지되어 있어 AES-256-GCM으로 암호화해서 저장.
@Component
@RequiredArgsConstructor
public class BillingKeyEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private final PortOneProperties portOneProperties;

    // 빌링키 원문을 AES-256-GCM으로 암호화해 Base64 문자열로 반환.
    // IV(초기화 벡터)는 암호화할 때마다 새로 랜덤 생성해서 같은 값을 두 번 암호화해도 결과가 달라지게 함.
    // 복호화 때 필요하므로 암호문 앞에 그대로 붙여서 저장.
    public String encrypt(String plainBillingKey) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv); //12바이트 배열 생성

            Cipher cipher = Cipher.getInstance(ALGORITHM); // 초기화
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv)); // GCM 모드 적용
            // * GCM: AEAD방식, 암호화+무결성검증
            byte[] cipherText = cipher.doFinal(plainBillingKey.getBytes(StandardCharsets.UTF_8)); // UTF-8 인코딩

            // iv + cipher (복호화를 위함)
            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(result); // 인코딩 후 반환
        } catch (Exception e) {
            throw new IllegalStateException("빌링키 암호화에 실패했습니다.", e);
        }
    }

    // 저장된 암호문(앞부분 IV + 뒷부분 실제 암호문)을 분리해서 원래 빌링키 문자열로 복원.
    public String decrypt(String encryptedBillingKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBillingKey); //디코딩
            byte[] iv = new byte[IV_LENGTH];  // iv 추출
            byte[] cipherText = new byte[decoded.length - IV_LENGTH]; //cipher 추출
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
            System.arraycopy(decoded, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("빌링키 복호화에 실패했습니다.", e);
        }
    }

    // Properties에 저장된 Base64 비밀키를 디코딩하여 AES SecretKeySpec 객체로 반환.
    private SecretKeySpec secretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(portOneProperties.billingKeyEncryptionSecret());
        return new SecretKeySpec(keyBytes, "AES"); // AES 알고리즘
    }
}
