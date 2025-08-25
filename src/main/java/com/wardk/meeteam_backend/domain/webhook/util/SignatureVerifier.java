package com.wardk.meeteam_backend.domain.webhook.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class SignatureVerifier {
    /**
     * GitHub webhook 서명이 유효한지 검증합니다.
     * 
     * @param rawBody 원본 요청 바디 바이트 배열
     * @param headerSig X-Hub-Signature-256 헤더 값
     * @param secret GitHub webhook secret
     * @return 서명이 유효한 경우 true, 그렇지 않으면 false
     */
    public static boolean validateSignature(byte[] rawBody, String headerSig, String secret) {
        if (headerSig == null || !headerSig.startsWith("sha256=")) {
            return false;
        }
        
        String providedSignature = headerSig.substring("sha256=".length());
        byte[] calculatedHmac = calculateHmacSha256(rawBody, secret);
        String calculatedSignature = bytesToHex(calculatedHmac).toLowerCase();
        
        // 상수 시간 비교 (타이밍 공격 방지)
        return MessageDigest.isEqual(
            calculatedSignature.getBytes(StandardCharsets.UTF_8),
            providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
    
    /**
     * HMAC-SHA256 해시를 계산합니다.
     */
    private static byte[] calculateHmacSha256(byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 계산 중 오류 발생", e);
        }
    }
    
    /**
     * 바이트 배열을 16진수 문자열로 변환합니다.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
