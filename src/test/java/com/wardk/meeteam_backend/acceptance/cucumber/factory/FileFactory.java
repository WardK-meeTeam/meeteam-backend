package com.wardk.meeteam_backend.acceptance.cucumber.factory;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 파일 데이터 생성 팩토리
 */
@Component
public class FileFactory {

    /**
     * 지정된 크기의 파일 바이트 배열 생성
     */
    public byte[] createFileContent(long sizeInBytes) {
        byte[] content = new byte[(int) sizeInBytes];
        Arrays.fill(content, (byte) 0xFF);
        return content;
    }

    /**
     * 기본 크기(1KB)의 파일 바이트 배열 생성
     */
    public byte[] createDefaultFileContent() {
        return createFileContent(1024);
    }

    /**
     * 파일명으로부터 Content-Type 추출
     */
    public String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "exe" -> "application/octet-stream";
            case "zip" -> "application/zip";
            case "js" -> "application/javascript";
            default -> "application/octet-stream";
        };
    }

    /**
     * 프로필 수정 요청용 기본 회원 정보 생성
     */
    public Map<String, Object> createDefaultMemberInfo(String name) {
        Map<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("name", name);
        memberInfo.put("age", 25);
        memberInfo.put("gender", "MALE");
        memberInfo.put("subCategories", List.of("웹서버"));
        memberInfo.put("skills", List.of("Java"));
        memberInfo.put("isParticipating", true);
        return memberInfo;
    }

    /**
     * 프로젝트 수정 요청용 기본 프로젝트 정보 생성
     */
    public Map<String, Object> createDefaultProjectInfo(String projectName) {
        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("name", projectName);
        projectInfo.put("description", projectName + " 프로젝트입니다.");
        projectInfo.put("projectCategory", "ETC");
        projectInfo.put("platformCategory", "WEB");
        projectInfo.put("offlineRequired", false);
        projectInfo.put("status", "PLANNING");
        projectInfo.put("startDate", "2025-01-01");
        projectInfo.put("endDate", "2025-12-31");
        projectInfo.put("recruitments", List.of());
        projectInfo.put("skills", List.of());
        return projectInfo;
    }
}