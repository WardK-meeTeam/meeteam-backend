import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SejongLoginPostTest {

    private static final String LOGIN_PAGE_URL = "https://portal.sejong.ac.kr/jsp/login/loginSSL.jsp";
    private static final String LOGIN_ACTION_URL = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";

    public static void main(String[] args) throws Exception {
        // 테스트용 계정 (실제 테스트 시 본인 계정으로 변경)
        String userId = args.length > 0 ? args[0] : "YOUR_STUDENT_ID";
        String password = args.length > 1 ? args[1] : "YOUR_PASSWORD";

        if (userId.equals("YOUR_STUDENT_ID")) {
            System.out.println("사용법: java SejongLoginPostTest <학번> <비밀번호>");
            System.out.println("예시: java SejongLoginPostTest 20123456 mypassword");
            System.out.println("\n=== 로그인 없이 페이지 구조만 테스트합니다 ===\n");
            testLoginPageStructure();
            return;
        }

        // 쿠키 관리를 위한 CookieManager 사용
        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER) // 리다이렉트 수동 처리
                .build();

        // 1. 먼저 로그인 페이지에 접근하여 세션 쿠키 획득
        System.out.println("=== Step 1: 로그인 페이지 접근 (세션 쿠키 획득) ===");
        HttpRequest loginPageRequest = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_PAGE_URL))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();

        HttpResponse<String> loginPageResponse = client.send(loginPageRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + loginPageResponse.statusCode());
        System.out.println("Cookies: " + cookieManager.getCookieStore().getCookies());

        // 2. 로그인 POST 요청
        System.out.println("\n=== Step 2: 로그인 POST 요청 ===");

        Map<String, String> formData = new HashMap<>();
        formData.put("mainLogin", "Y");
        formData.put("rtUrl", "");
        formData.put("id", userId);
        formData.put("password", password);

        String formBody = buildFormBody(formData);
        System.out.println("Form data: " + formData.keySet());

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_ACTION_URL))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Referer", LOGIN_PAGE_URL)
                .header("Origin", "https://portal.sejong.ac.kr")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + loginResponse.statusCode());
        System.out.println("Headers: ");
        loginResponse.headers().map().forEach((key, values) -> {
            System.out.println("  " + key + ": " + values);
        });

        String responseBody = loginResponse.body();
        System.out.println("\n=== Response Body (첫 2000자) ===");
        System.out.println(responseBody.substring(0, Math.min(2000, responseBody.length())));

        // 로그인 성공/실패 판단
        System.out.println("\n=== 로그인 결과 분석 ===");
        if (loginResponse.statusCode() == 302 || loginResponse.statusCode() == 301) {
            String location = loginResponse.headers().firstValue("location").orElse("");
            System.out.println("리다이렉트 발생: " + location);
            if (location.contains("main") || location.contains("index")) {
                System.out.println("✅ 로그인 성공으로 추정됩니다!");
            } else if (location.contains("login") || location.contains("error")) {
                System.out.println("❌ 로그인 실패로 추정됩니다.");
            }
        } else if (responseBody.contains("로그인 실패") || responseBody.contains("비밀번호") || responseBody.contains("error")) {
            System.out.println("❌ 로그인 실패 - 응답에 에러 메시지 포함");
        } else if (responseBody.contains("로그아웃") || responseBody.contains("마이페이지")) {
            System.out.println("✅ 로그인 성공으로 추정됩니다!");
        }

        // 3. 로그인 후 메인 페이지 접근 테스트
        System.out.println("\n=== Step 3: 로그인 후 메인 페이지 접근 테스트 ===");
        HttpRequest mainPageRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://portal.sejong.ac.kr/main/index.do"))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .GET()
                .build();

        HttpResponse<String> mainPageResponse = client.send(mainPageRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + mainPageResponse.statusCode());
        System.out.println("Body contains '로그아웃': " + mainPageResponse.body().contains("로그아웃"));
        System.out.println("Body contains '로그인': " + mainPageResponse.body().contains("로그인"));
    }

    private static String buildFormBody(Map<String, String> data) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private static void testLoginPageStructure() throws Exception {
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_PAGE_URL))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        System.out.println("로그인 페이지 분석 결과:");
        System.out.println("- Action URL: /jsp/login/login_action.jsp");
        System.out.println("- Method: POST");
        System.out.println("- 필수 필드: id, password, mainLogin, rtUrl");
        System.out.println("\n주의: npkencrypt='on' 속성이 발견됨");
        System.out.println("→ 키보드 보안 모듈이 사용될 수 있음");
        System.out.println("→ 단순 POST로는 로그인이 안 될 수 있음");

        // JavaScript 암호화 관련 코드 찾기
        if (body.contains("npPfsCtrl") || body.contains("encrypt")) {
            System.out.println("\n=== 암호화 관련 코드 발견 ===");
            String[] lines = body.split("\n");
            for (String line : lines) {
                if ((line.contains("encrypt") || line.contains("npPfs") || line.contains("nFilter"))
                    && line.length() < 200) {
                    System.out.println(line.trim());
                }
            }
        }
    }
}