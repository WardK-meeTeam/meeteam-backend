import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SejongLoginTest {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // 1. SSL 로그인 페이지 접근
        System.out.println("=== 세종대 포털 SSL 로그인 페이지 접근 ===");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://portal.sejong.ac.kr/jsp/login/loginSSL.jsp"))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        System.out.println("Headers: " + response.headers().map());

        String body = response.body();
        System.out.println("Body length: " + body.length());

        // Form 분석
        System.out.println("\n=== Form 분석 ===");

        // form 태그 찾기
        int formStart = body.toLowerCase().indexOf("<form");
        while (formStart != -1) {
            int formEnd = body.indexOf(">", formStart);
            System.out.println("Form: " + body.substring(formStart, Math.min(formEnd + 1, formStart + 200)));
            formStart = body.toLowerCase().indexOf("<form", formEnd);
        }

        // input 필드 찾기
        System.out.println("\n=== Input Fields ===");
        String[] parts = body.split("(?i)<input");
        for (int i = 1; i < parts.length; i++) {
            int end = parts[i].indexOf(">");
            if (end > -1 && end < 300) {
                String input = "<input" + parts[i].substring(0, end + 1);
                if (input.contains("name=") || input.contains("id=")) {
                    System.out.println(input.replaceAll("\\s+", " ").trim());
                }
            }
        }

        // action URL 찾기
        System.out.println("\n=== Action URL ===");
        int actionIdx = body.toLowerCase().indexOf("action=");
        while (actionIdx != -1) {
            int start = actionIdx;
            int end = Math.min(body.indexOf(" ", actionIdx + 10), body.indexOf(">", actionIdx));
            if (end > start && end < start + 200) {
                System.out.println(body.substring(start, end));
            }
            actionIdx = body.toLowerCase().indexOf("action=", actionIdx + 1);
        }

        // loginSSL.jsp 관련 스크립트 찾기
        System.out.println("\n=== SSL/Login Script ===");
        if (body.contains("loginSSL")) {
            int idx = body.indexOf("loginSSL");
            System.out.println("Found loginSSL: " + body.substring(Math.max(0, idx - 50), Math.min(body.length(), idx + 100)));
        }

        // submit 관련 코드 찾기
        if (body.contains("submit")) {
            System.out.println("\n=== Submit 관련 ===");
            String[] lines = body.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("submit") && line.length() < 200) {
                    System.out.println(line.trim());
                }
            }
        }
    }
}
