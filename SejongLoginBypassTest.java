import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 세종대 포털 로그인 테스트
 * - npPfsIgnore 쿠키를 설정하여 키보드 보안 우회 시도
 * - 단순 POST 요청으로 로그인 테스트
 */
public class SejongLoginBypassTest {

    private static final String LOGIN_PAGE_URL = "https://portal.sejong.ac.kr/jsp/login/loginSSL.jsp";
    private static final String LOGIN_ACTION_URL = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";

    public static void main(String[] args) throws Exception {
        String userId = args.length > 0 ? args[0] : null;
        String password = args.length > 1 ? args[1] : null;

        if (userId == null || password == null) {
            System.out.println("사용법: java SejongLoginBypassTest <학번> <비밀번호>");
            return;
        }

        // 쿠키 관리
        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        // Step 1: 로그인 페이지 접근
        System.out.println("=== Step 1: 로그인 페이지 접근 ===");
        HttpRequest pageRequest = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_PAGE_URL))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .GET()
                .build();

        HttpResponse<String> pageResponse = client.send(pageRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + pageResponse.statusCode());

        // npPfsIgnore 쿠키 추가 (키보드 보안 우회 시도)
        HttpCookie bypassCookie = new HttpCookie("npPfsIgnore", "true");
        bypassCookie.setPath("/");
        bypassCookie.setDomain("portal.sejong.ac.kr");
        cookieManager.getCookieStore().add(URI.create("https://portal.sejong.ac.kr"), bypassCookie);

        System.out.println("Cookies: " + cookieManager.getCookieStore().getCookies());

        // Step 2: 로그인 POST 요청
        System.out.println("\n=== Step 2: 로그인 POST 요청 ===");

        Map<String, String> formData = new HashMap<>();
        formData.put("mainLogin", "Y");
        formData.put("rtUrl", "");
        formData.put("id", userId);
        formData.put("password", password);

        String formBody = buildFormBody(formData);

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_ACTION_URL))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Referer", LOGIN_PAGE_URL)
                .header("Origin", "https://portal.sejong.ac.kr")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + loginResponse.statusCode());
        System.out.println("\nResponse Headers:");
        loginResponse.headers().map().forEach((key, values) -> {
            if (key.toLowerCase().contains("location") || key.toLowerCase().contains("cookie")) {
                System.out.println("  " + key + ": " + values);
            }
        });

        String body = loginResponse.body();
        System.out.println("\n=== Response Body ===");
        System.out.println(body.substring(0, Math.min(3000, body.length())));

        // 로그인 결과 분석
        analyzeResult(loginResponse, body);

        // Step 3: 리다이렉트 따라가기
        if (loginResponse.statusCode() == 302 || loginResponse.statusCode() == 301) {
            String location = loginResponse.headers().firstValue("location").orElse("");
            if (!location.isEmpty()) {
                System.out.println("\n=== Step 3: 리다이렉트 따라가기 ===");
                if (!location.startsWith("http")) {
                    location = "https://portal.sejong.ac.kr" + location;
                }
                System.out.println("Following redirect to: " + location);

                HttpRequest redirectRequest = HttpRequest.newBuilder()
                        .uri(URI.create(location))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET()
                        .build();

                HttpResponse<String> redirectResponse = client.send(redirectRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("Status: " + redirectResponse.statusCode());
                System.out.println("Body preview: " + redirectResponse.body().substring(0, Math.min(500, redirectResponse.body().length())));
            }
        }
    }

    private static void analyzeResult(HttpResponse<String> response, String body) {
        System.out.println("\n=== 로그인 결과 분석 ===");

        if (response.statusCode() == 302) {
            String location = response.headers().firstValue("location").orElse("");
            System.out.println("302 Redirect to: " + location);

            if (location.contains("main") || location.contains("index") || location.contains("portal")) {
                System.out.println("✅ 로그인 성공 가능성 높음!");
            } else if (location.contains("login") || location.contains("error") || location.contains("fail")) {
                System.out.println("❌ 로그인 실패로 추정");
            }
        } else if (response.statusCode() == 200) {
            if (body.contains("alert") || body.contains("실패") || body.contains("오류") || body.contains("틀렸습니다")) {
                System.out.println("❌ 로그인 실패 - 에러 메시지 감지");
                // 에러 메시지 추출
                if (body.contains("alert(")) {
                    int start = body.indexOf("alert(");
                    int end = body.indexOf(")", start);
                    if (end > start) {
                        System.out.println("Alert 메시지: " + body.substring(start, end + 1));
                    }
                }
            } else if (body.contains("로그아웃") || body.contains("마이페이지")) {
                System.out.println("✅ 로그인 성공!");
            } else {
                System.out.println("⚠️ 결과 불명확 - 응답 내용 확인 필요");
            }
        }
    }

    private static String buildFormBody(Map<String, String> data) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (builder.length() > 0) builder.append("&");
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                   .append("=")
                   .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return builder.toString();
    }
}
