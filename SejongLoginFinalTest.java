import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SejongLoginFinalTest {
    public static void main(String[] args) throws Exception {
        String userId = "21013220";
        String password = "19980611";

        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();

        System.out.println("=== 세종대 포털 로그인 테스트 ===");
        System.out.println("ID: " + userId);

        // Step 1: 로그인 페이지 접근하여 세션 쿠키 획득
        System.out.println("\n[Step 1] 로그인 페이지 접근...");
        HttpRequest pageRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://portal.sejong.ac.kr/jsp/login/loginSSL.jsp"))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .GET()
                .build();

        HttpResponse<String> pageResponse = client.send(pageRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("세션 쿠키 획득: " + cookieManager.getCookieStore().getCookies().size() + "개");

        // Step 2: 로그인 POST 요청
        System.out.println("\n[Step 2] 로그인 요청...");
        String formData = "id=" + URLEncoder.encode(userId, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                + "&mainLogin=Y&rtUrl=";

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://portal.sejong.ac.kr/jsp/login/login_action.jsp"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Referer", "https://portal.sejong.ac.kr/jsp/login/loginSSL.jsp")
                .header("Origin", "https://portal.sejong.ac.kr")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());

        String body = response.body();

        // 결과 확인
        if (body.contains("result = 'OK'")) {
            System.out.println("\n✅ 로그인 성공!");

            // SSO 토큰 확인
            response.headers().map().forEach((key, values) -> {
                if (key.toLowerCase().contains("cookie")) {
                    for (String v : values) {
                        if (v.contains("ssotoken")) {
                            System.out.println("SSO Token 발급됨!");
                        }
                    }
                }
            });

            // Step 3: 로그인 후 메인 페이지 접근 확인
            System.out.println("\n[Step 3] 메인 페이지 접근 확인...");
            HttpRequest mainRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://portal.sejong.ac.kr/user/index.do"))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> mainResponse = client.send(mainRequest, HttpResponse.BodyHandlers.ofString());
            String mainBody = mainResponse.body();

            if (mainBody.contains("로그아웃") || mainBody.contains("logout")) {
                System.out.println("✅ 메인 페이지 접근 성공! (로그아웃 버튼 확인)");
            }

        } else if (body.contains("erridpwd")) {
            System.out.println("\n❌ 로그인 실패: 아이디 또는 비밀번호 불일치");
        } else if (body.contains("noaccess")) {
            System.out.println("\n❌ 접근 차단됨");
            System.out.println("응답: " + body);
        } else {
            System.out.println("\n응답 내용:");
            System.out.println(body.substring(0, Math.min(500, body.length())));
        }
    }
}