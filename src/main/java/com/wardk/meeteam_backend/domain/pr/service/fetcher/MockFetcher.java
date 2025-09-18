//package com.wardk.meeteam_backend.domain.pr.service.fetcher;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.wardk.meeteam_backend.web.pr.dto.PrData;
//import com.wardk.meeteam_backend.web.pr.dto.PrFileData;
//
//import java.util.List;
//
////@Component
//public class MockFetcher implements PullRequestFetcher {
//    @Override
//    public PrData getPr(String repoFullName, int prNumber, JsonNode webhookPayload) {
//        return new PrData(
//                repoFullName,
//                prNumber,
//                "[MOCK] 제목",
//                "[MOCK] 본문",
//                "open",
//                false,
//                false,
//                "main",
//                "feature/mock",
//                "deadbeefdeadbeefdeadbeef",
//                "mock-user",
//                10,
//                2,
//                3,
//                1,
//                0
//        );
//    }
//
//    @Override
//    public List<PrFileData> listFiles(String repoFullName, int prNumber) {
//        return List.of(
//                new PrFileData("src/App.java", "modified", 10, 2, 12, "blobsha-app", null, 120,
//                        "@@ -1,2 +1,10 @@\n- old\n+ new"),
//                new PrFileData("README.md", "added", 20, 0, 20, "blobsha-readme", null, 20,
//                        "@@ -0,0 +1,20 @@\n+ readme")
//        );
//    }
//}
