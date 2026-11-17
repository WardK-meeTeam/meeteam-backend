package com.wardk.meeteam_backend.loadtest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

/**
 * 부하 테스트용 JWT 액세스 토큰 사전 발급기.
 *
 * <p>로그인이 세종대 포털(외부 시스템)에 의존하므로 부하 테스트에서는 실제 로그인 대신
 * 서버와 동일한 시크릿/클레임 구조로 토큰을 사전 발급해 파일로 떨어뜨린다(토큰 팩토리 패턴).
 * 생성된 loadtest/tokens.json 은 k6 SharedArray 로 읽는다.</p>
 *
 * <p>클레임 구조는 {@code JwtUtil.createToken()} 과 동일해야 한다.
 * 단, id 클레임은 JwtFilter 가 인증에 사용하지 않으므로(이메일로 DB 조회) 순번으로 채운다.</p>
 *
 * <p>실행: {@code ./gradlew generateLoadTestTokens -Pcount=10000}</p>
 */
public class LoadTestTokenGenerator {

    /** application-local.yml 의 jwt.secret-key 와 동일해야 서버가 서명을 검증할 수 있다. */
    private static final String DEFAULT_SECRET =
            "zcR9P80KqqLmF5hT8dQ7VzXwJJgNZp3qrTy4FcRNm8L9H2zBxPVwP7L8RcNF5G2Q3xAW9cK6d";

    private static final long EXP_MILLIS = 24L * 60 * 60 * 1000; // 24시간

    public static void main(String[] args) throws Exception {
        int count = Integer.parseInt(System.getProperty("count", "10000"));
        String secret = System.getProperty("secret", DEFAULT_SECRET);

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        Path out = Path.of("loadtest/tokens.json");
        Files.createDirectories(out.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(out)) {
            writer.write("[\n");
            for (int i = 1; i <= count; i++) {
                String email = "loadtest%05d@loadtest.local".formatted(i);
                String token = createAccessToken(key, email, (long) i);
                writer.write("  {\"email\": \"%s\", \"token\": \"%s\"}%s\n"
                        .formatted(email, token, i < count ? "," : ""));
            }
            writer.write("]\n");
        }

        System.out.printf("토큰 %d개 생성 완료 → %s%n", count, out.toAbsolutePath());
    }

    private static String createAccessToken(SecretKey key, String email, Long memberId) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("category", "access")
                .claim("username", email)
                .claim("id", memberId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXP_MILLIS))
                .signWith(key)
                .compact();
    }
}