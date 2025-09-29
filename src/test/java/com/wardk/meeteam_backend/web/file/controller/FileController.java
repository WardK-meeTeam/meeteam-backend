//package com.wardk.meeteam_backend.web.file.controller;
//
//import com.wardk.meeteam_backend.domain.file.entity.UploadedFile;
//import com.wardk.meeteam_backend.domain.file.service.S3FileService;
//import com.wardk.meeteam_backend.global.response.SuccessResponse;
//import com.wardk.meeteam_backend.web.file.dto.FileListResponse;
//import com.wardk.meeteam_backend.web.file.dto.FileUploadResponse;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/v1/files")
//@Tag(name = "File Upload", description = "파일 업로드 관련 API")
//public class FileController {
//
//    private final S3FileService s3FileService;
//
//    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(summary = "이미지 파일 업로드", description = "이미지 파일을 S3에 업로드합니다.")
//    public ResponseEntity<SuccessResponse<FileUploadResponse>> uploadImage(
//            @Parameter(description = "업로드할 이미지 파일", required = true)
//            @RequestParam("file") MultipartFile file,
//            @Parameter(description = "업로드할 디렉토리 (기본값: images)", required = false)
//            @RequestParam(value = "directory", defaultValue = "images") String directory) {
//
//        log.info("이미지 업로드 요청 - 파일명: {}, 크기: {}, 디렉토리: {}",
//                file.getOriginalFilename(), file.getSize(), directory);
//
//        String fileUrl = s3FileService.uploadFile(file, directory);
//
//        FileUploadResponse response = FileUploadResponse.of(
//                extractFileNameFromUrl(fileUrl),
//                file.getOriginalFilename(),
//                fileUrl,
//                file.getContentType(),
//                file.getSize()
//        );
//
//        return ResponseEntity.ok(SuccessResponse.onSuccess(response));
//    }
//
//    @PostMapping(value = "/upload/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(summary = "프로필 이미지 업로드", description = "프로필 이미지를 S3에 업로드합니다.")
//    public ResponseEntity<SuccessResponse<FileUploadResponse>> uploadProfileImage(
//            @Parameter(description = "업로드할 프로필 이미지 파일", required = true)
//            @RequestParam("file") MultipartFile file) {
//
//        log.info("프로필 이미지 업로드 요청 - 파일명: {}, 크기: {}",
//                file.getOriginalFilename(), file.getSize());
//
//        String fileUrl = s3FileService.uploadFile(file, "profiles");
//
//        FileUploadResponse response = FileUploadResponse.of(
//                extractFileNameFromUrl(fileUrl),
//                file.getOriginalFilename(),
//                fileUrl,
//                file.getContentType(),
//                file.getSize()
//        );
//
//        return ResponseEntity.ok(SuccessResponse.onSuccess(response));
//    }
//
//    @PostMapping(value = "/upload/project", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(summary = "프로젝트 이미지 업로드", description = "프로젝트 관련 이미지를 S3에 업로드합니다.")
//    public ResponseEntity<SuccessResponse<FileUploadResponse>> uploadProjectImage(
//            @Parameter(description = "업로드할 프로젝트 이미지 파일", required = true)
//            @RequestParam("file") MultipartFile file) {
//
//        log.info("프로젝트 이미지 업로드 요청 - 파일명: {}, 크기: {}",
//                file.getOriginalFilename(), file.getSize());
//
//        String fileUrl = s3FileService.uploadFile(file, "projects");
//
//        FileUploadResponse response = FileUploadResponse.of(
//                extractFileNameFromUrl(fileUrl),
//                file.getOriginalFilename(),
//                fileUrl,
//                file.getContentType(),
//                file.getSize()
//        );
//
//        return ResponseEntity.ok(SuccessResponse.onSuccess(response));
//    }
//
//    @DeleteMapping("/delete")
//    @Operation(summary = "파일 삭제", description = "S3에서 파일을 삭제합니다.")
//    public ResponseEntity<SuccessResponse<String>> deleteFile(
//            @Parameter(description = "삭제할 파일의 URL", required = true)
//            @RequestParam("fileUrl") String fileUrl) {
//
//        log.info("파일 삭제 요청 - URL: {}", fileUrl);
//
//        s3FileService.deleteFile(fileUrl);
//
//        return ResponseEntity.ok(SuccessResponse.onSuccess("성공적 삭제"));
//    }
//
//    @GetMapping("/list")
//    @Operation(summary = "업로드된 파일 목록 조회", description = "특정 디렉토리의 파일 목록을 조회합니다.")
//    public ResponseEntity<SuccessResponse<List<FileListResponse>>> getFileList(
//            @Parameter(description = "조회할 디렉토리명")
//            @RequestParam(value = "directory", required = false) String directory) {
//
//        log.info("파일 목록 조회 요청 - 디렉토리: {}", directory);
//
//        List<UploadedFile> files = directory != null ?
//                s3FileService.getFilesByDirectory(directory) :
//                s3FileService.getAllFiles();
//
//        List<FileListResponse> response = files.stream()
//                .map(FileListResponse::from)
//                .toList();
//
//        return ResponseEntity.ok(SuccessResponse.onSuccess(response));
//    }
//
//    @GetMapping("/my-files")
//    @Operation(summary = "내가 업로드한 파일 목록 조회", description = "현재 사용자가 업로드한 파일 목록을 조회합니다.")
//    public ResponseEntity<SuccessResponse<List<FileListResponse>>> getMyFiles(
//            @Parameter(description = "사용자 ID", hidden = true)
//            @RequestParam("uploaderId") Long uploaderId) {
//
//        log.info("사용자 파일 목록 조회 요청 - 사용자 ID: {}", uploaderId);
//
//        List<UploadedFile> files = s3FileService.getFilesByUploader(uploaderId);
//
//        List<FileListResponse> response = files.stream()
//                .map(FileListResponse::from)
//                .toList();
//
//        return ResponseEntity.ok(SuccessResponse.onSuccess(response));
//    }
//
//    private String extractFileNameFromUrl(String fileUrl) {
//        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
//    }
//}
