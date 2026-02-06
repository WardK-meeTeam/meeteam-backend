# S3 파일 업로드 전략 분석

## 1. 현재 아키텍처 분석

### 현재 구조: Client → Server → S3 (Proxy Upload)

```
┌─────────┐     MultipartFile      ┌─────────────┐    PutObject    ┌─────┐
│ Client  │ ───────────────────────▶ │ Spring Boot │ ────────────────▶ │ S3  │
└─────────┘                          └─────────────┘                  └─────┘
                                            │
                                            ▼
                                    ┌─────────────┐
                                    │  Database   │
                                    │ (metadata)  │
                                    └─────────────┘
```

### 현재 구현 위치
- **S3Config**: `global/config/S3Config.java`
- **S3FileService**: `domain/file/service/S3FileService.java`
- **업로드 엔드포인트**:
  - `POST /api/projects` (프로젝트 이미지)
  - `PUT /api/members` (프로필 이미지)

### 현재 구현의 특징
- AWS SDK v1 (1.12.470) 사용
- 파일 검증: 확장자(jpg, jpeg, png, gif, webp), 크기(10MB)
- UUID 기반 파일명 생성
- DB에 메타데이터 저장 (`uploaded_files` 테이블)

---

## 2. 대안 아키텍처: Pre-signed URL 방식

### 구조: Client → S3 직접 업로드 (Direct Upload)

```
┌─────────┐  1. URL 요청   ┌─────────────┐
│ Client  │ ──────────────▶ │ Spring Boot │
└─────────┘                 └─────────────┘
     │                             │
     │                             │ 2. Pre-signed URL 생성
     │                             ▼
     │                      ┌─────────────┐
     │                      │     S3      │
     │                      └─────────────┘
     │                             │
     │    3. Pre-signed URL 반환   │
     │ ◀───────────────────────────┘
     │
     │    4. 파일 직접 업로드 (PUT)
     └─────────────────────────────▶ S3

     │    5. 업로드 완료 확인
     └─────────────────────────────▶ Spring Boot
                                          │
                                          ▼
                                   ┌─────────────┐
                                   │  Database   │
                                   │ (metadata)  │
                                   └─────────────┘
```

---

## 3. 방식별 장단점 비교

### 현재 방식 (Server Proxy Upload)

| 구분 | 내용 |
|------|------|
| **장점** | |
| 보안 | 서버에서 파일 내용을 완전히 검증 가능 (바이러스, 악성코드 스캔) |
| 일관성 | 모든 비즈니스 로직이 서버에서 처리됨 |
| 단순성 | 클라이언트 구현이 단순함 (일반적인 multipart 업로드) |
| 제어력 | 업로드 진행 상황, 실패 처리 등을 서버에서 완전 제어 |
| 메타데이터 | 파일과 메타데이터를 트랜잭션으로 함께 처리 가능 |
| **단점** | |
| 서버 부하 | 모든 파일이 서버 메모리/디스크를 거침 (현재 최대 10MB) |
| 대역폭 비용 | 파일이 서버를 경유하므로 네트워크 비용 2배 |
| 확장성 | 동시 업로드 증가 시 서버 병목 발생 가능 |
| 지연시간 | 서버 경유로 인한 추가 latency |

### Pre-signed URL 방식 (Direct Upload)

| 구분 | 내용 |
|------|------|
| **장점** | |
| 서버 부하 감소 | 파일 데이터가 서버를 거치지 않음 |
| 확장성 | S3의 확장성을 직접 활용 (무제한에 가까운 동시 업로드) |
| 비용 절감 | 서버 대역폭 비용 절감, EC2 인스턴스 부하 감소 |
| 대용량 지원 | 5GB까지 단일 PUT, Multipart로 5TB까지 가능 |
| 성능 | S3 직접 연결로 업로드 속도 향상 |
| **단점** | |
| 보안 제한 | 서버에서 파일 내용 검증 불가 (업로드 후 검증 필요) |
| 복잡성 | 클라이언트 구현 복잡도 증가 |
| 일관성 | 업로드 성공 후 메타데이터 저장 실패 시 orphan 파일 발생 가능 |
| CORS | S3 버킷 CORS 설정 필요 |
| 클라이언트 제어 | 파일명, 확장자 등 클라이언트가 조작 가능 |

---

## 4. 정량적 비교

### 시나리오: 5MB 이미지 100개 동시 업로드

| 항목 | Server Proxy | Pre-signed URL |
|------|--------------|----------------|
| 서버 메모리 사용 | ~500MB+ | ~수 KB (URL 생성만) |
| 서버 네트워크 I/O | 1GB (500MB in + 500MB out) | 무시 가능 |
| 업로드 완료 시간 | 서버 병목 발생 가능 | S3 직접 연결 |
| EC2 비용 영향 | 높음 (CPU, 메모리, 네트워크) | 낮음 |

### 비용 추정 (월 10,000건 업로드, 평균 5MB)

| 항목 | Server Proxy | Pre-signed URL |
|------|--------------|----------------|
| S3 PUT 요청 | $0.05 | $0.05 |
| S3 데이터 전송 | $4.50 | $4.50 |
| EC2 데이터 전송 (추가) | ~$4.50 | $0 |
| EC2 인스턴스 부하 | 높음 | 낮음 |

---

## 5. 하이브리드 전략 (권장)

### 개요
파일 크기와 용도에 따라 두 방식을 혼합 사용

```
┌─────────────────────────────────────────────────────────────┐
│                       Upload Strategy                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   파일 크기 < 1MB (프로필 이미지, 썸네일)                    │
│   └──▶ Server Proxy 방식 유지                               │
│        - 즉시 이미지 처리 (리사이즈, 압축)                   │
│        - 메타데이터 트랜잭션 보장                            │
│                                                              │
│   파일 크기 >= 1MB (프로젝트 이미지, 첨부파일)              │
│   └──▶ Pre-signed URL 방식                                  │
│        - 서버 부하 감소                                      │
│        - 클라이언트 직접 업로드                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 흐름도

```
                    ┌─────────────────────┐
                    │  Upload Request     │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │  File Size Check    │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                                 ▼
     ┌────────────────┐               ┌────────────────┐
     │   < 1MB        │               │   >= 1MB       │
     │ Server Proxy   │               │ Pre-signed URL │
     └───────┬────────┘               └───────┬────────┘
             │                                 │
             ▼                                 ▼
     ┌────────────────┐               ┌────────────────┐
     │ 1. 파일 검증    │               │ 1. URL 요청    │
     │ 2. 이미지 처리  │               │ 2. URL 생성    │
     │ 3. S3 업로드   │               │ 3. 클라이언트   │
     │ 4. DB 저장     │               │    직접 업로드  │
     └───────┬────────┘               │ 4. 완료 콜백   │
             │                        │ 5. DB 저장     │
             │                        └───────┬────────┘
             │                                 │
             └─────────────┬───────────────────┘
                           │
                           ▼
                    ┌─────────────────────┐
                    │    Upload Complete  │
                    └─────────────────────┘
```

---

## 6. Pre-signed URL 구현 가이드

### 6.1 AWS SDK v2 마이그레이션 (선택사항)

```groovy
// build.gradle
implementation platform('software.amazon.awssdk:bom:2.21.0')
implementation 'software.amazon.awssdk:s3'
implementation 'software.amazon.awssdk:s3-transfer-manager'
```

### 6.2 Pre-signed URL 생성 서비스

```java
@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 업로드용 Pre-signed URL 생성
     * @param directory 저장 디렉토리 (images, profiles)
     * @param originalFileName 원본 파일명
     * @param contentType MIME 타입
     * @param expirationMinutes URL 유효 시간 (분)
     * @return Pre-signed URL 정보
     */
    public PresignedUrlResponse generateUploadUrl(
            String directory,
            String originalFileName,
            String contentType,
            int expirationMinutes
    ) {
        // 파일명 생성 (UUID 기반)
        String extension = extractExtension(originalFileName);
        validateExtension(extension);
        String fileName = UUID.randomUUID() + "." + extension;
        String key = directory + "/" + fileName;

        // 만료 시간 설정
        Date expiration = new Date();
        expiration.setTime(expiration.getTime() + expirationMinutes * 60 * 1000);

        // Pre-signed URL 생성
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration)
                .withContentType(contentType);

        URL presignedUrl = amazonS3.generatePresignedUrl(request);

        return PresignedUrlResponse.builder()
                .uploadUrl(presignedUrl.toString())
                .fileKey(key)
                .fileName(fileName)
                .expiresAt(expiration.toInstant())
                .build();
    }

    /**
     * 다운로드용 Pre-signed URL 생성 (private 버킷용)
     */
    public String generateDownloadUrl(String key, int expirationMinutes) {
        Date expiration = new Date();
        expiration.setTime(expiration.getTime() + expirationMinutes * 60 * 1000);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        return amazonS3.generatePresignedUrl(request).toString();
    }
}
```

### 6.3 업로드 완료 확인 엔드포인트

```java
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final S3PresignedUrlService presignedUrlService;
    private final S3FileService s3FileService;

    /**
     * Pre-signed URL 요청
     */
    @PostMapping("/presigned-url")
    public SuccessResponse<PresignedUrlResponse> getPresignedUrl(
            @RequestBody @Valid PresignedUrlRequest request,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        PresignedUrlResponse response = presignedUrlService.generateUploadUrl(
                request.getDirectory(),
                request.getFileName(),
                request.getContentType(),
                15 // 15분 유효
        );
        return SuccessResponse.of(SuccessCode._OK, response);
    }

    /**
     * 업로드 완료 확인 및 메타데이터 저장
     */
    @PostMapping("/confirm")
    public SuccessResponse<FileUploadResponse> confirmUpload(
            @RequestBody @Valid UploadConfirmRequest request,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        // S3에 파일 존재 확인
        if (!s3FileService.existsInS3(request.getFileKey())) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        // 메타데이터 저장
        FileUploadResponse response = s3FileService.saveFileMetadata(
                request.getFileKey(),
                request.getOriginalFileName(),
                request.getContentType(),
                request.getFileSize(),
                userDetails.getMemberId()
        );

        return SuccessResponse.of(SuccessCode.FILE_UPLOADED, response);
    }
}
```

### 6.4 S3 CORS 설정

```json
{
    "CORSRules": [
        {
            "AllowedHeaders": ["*"],
            "AllowedMethods": ["PUT", "POST", "GET"],
            "AllowedOrigins": [
                "https://meeteam.com",
                "http://localhost:3000"
            ],
            "ExposeHeaders": ["ETag"],
            "MaxAgeSeconds": 3600
        }
    ]
}
```

### 6.5 클라이언트 구현 예시 (React/TypeScript)

```typescript
interface PresignedUrlResponse {
  uploadUrl: string;
  fileKey: string;
  fileName: string;
  expiresAt: string;
}

async function uploadWithPresignedUrl(file: File, directory: string): Promise<string> {
  // 1. Pre-signed URL 요청
  const presignedResponse = await fetch('/api/v1/files/presigned-url', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      directory,
      fileName: file.name,
      contentType: file.type
    })
  });

  const { uploadUrl, fileKey, fileName } = await presignedResponse.json();

  // 2. S3 직접 업로드
  await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type },
    body: file
  });

  // 3. 업로드 완료 확인
  await fetch('/api/v1/files/confirm', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fileKey,
      originalFileName: file.name,
      contentType: file.type,
      fileSize: file.size
    })
  });

  return `https://${bucket}.s3.${region}.amazonaws.com/${fileKey}`;
}
```

---

## 7. 보안 고려사항

### 7.1 Pre-signed URL 방식의 보안 강화

| 위협 | 대응 방안 |
|------|----------|
| URL 재사용 | 짧은 만료 시간 (5-15분) 설정 |
| 파일 타입 우회 | Content-Type 조건 추가, 업로드 후 검증 |
| 대용량 공격 | Content-Length 제한 조건 추가 |
| 인증되지 않은 접근 | URL 요청 시 인증 필수 |
| orphan 파일 | Lambda + S3 이벤트로 주기적 정리 |

### 7.2 추가 보안 레이어

```java
// Pre-signed URL에 조건 추가
GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key)
        .withMethod(HttpMethod.PUT)
        .withExpiration(expiration)
        .withContentType(contentType);

// Content-Length 제한 (10MB)
request.putCustomRequestHeader("x-amz-meta-max-size", "10485760");
```

### 7.3 업로드 후 검증 (Lambda 트리거)

```
S3 PutObject Event → Lambda → 파일 검증 → 실패 시 삭제
```

---

## 8. 마이그레이션 전략

### Phase 1: 인프라 준비 (1-2일)
- [ ] S3 CORS 설정
- [ ] Pre-signed URL 서비스 구현
- [ ] 기존 API와 병행 운영 가능하도록 새 엔드포인트 추가

### Phase 2: 클라이언트 업데이트 (2-3일)
- [ ] 프론트엔드 업로드 로직 수정
- [ ] 에러 핸들링 및 재시도 로직 구현
- [ ] 업로드 진행률 표시 (선택)

### Phase 3: 모니터링 및 최적화 (1주)
- [ ] CloudWatch 메트릭 모니터링
- [ ] orphan 파일 정리 로직 구현
- [ ] 성능 지표 비교 분석

### Phase 4: 기존 방식 deprecation (2주 후)
- [ ] 기존 multipart 업로드 엔드포인트 제거
- [ ] 문서 업데이트

---

## 9. 결론 및 권장사항

### MeeTeam 프로젝트에 대한 권장사항

| 항목 | 권장사항 | 이유 |
|------|----------|------|
| **즉시 적용** | 현재 방식 유지 | 현재 트래픽 규모에서는 서버 부하 문제 없음 |
| **중기 계획** | Pre-signed URL 도입 준비 | 확장성 확보, 비용 최적화 |
| **우선 적용 대상** | 프로젝트 이미지 업로드 | 상대적으로 큰 파일 (최대 10MB) |
| **유지** | 프로필 이미지 | 작은 파일, 즉시 처리 필요 |

### 도입 시점 기준

Pre-signed URL 도입을 고려해야 하는 시점:
1. **일일 업로드 1,000건 이상** 시
2. **동시 업로드 50건 이상** 발생 시
3. **EC2 인스턴스 메모리 사용률 70% 이상** 지속 시
4. **대용량 파일 업로드 요구** (영상, 문서 등) 발생 시

### 최종 권장 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                    MeeTeam File Upload Architecture              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────┐                    ┌─────────────┐                 │
│  │ Client  │                    │ Spring Boot │                 │
│  └────┬────┘                    └──────┬──────┘                 │
│       │                                │                         │
│       │  ┌──────────────────────────┐  │                         │
│       │  │     Profile Image        │  │                         │
│       │  │      (< 1MB)             │  │                         │
│       │  │  ────────────────────▶   │  │                         │
│       │  │   Server Proxy 방식      │──┼──▶ S3                   │
│       │  └──────────────────────────┘  │                         │
│       │                                │                         │
│       │  ┌──────────────────────────┐  │                         │
│       │  │    Project Image         │  │                         │
│       │  │     (>= 1MB)             │  │  Pre-signed URL         │
│       │  │  1. URL 요청 ──────────▶ │──┼──▶ 생성                 │
│       │  │  2. 직접 업로드 ─────────┼──┼──────────────────▶ S3   │
│       │  │  3. 완료 확인 ──────────▶│  │                         │
│       │  └──────────────────────────┘  │                         │
│       │                                │                         │
│       │                         ┌──────┴──────┐                  │
│       │                         │  Database   │                  │
│       │                         │ (metadata)  │                  │
│       │                         └─────────────┘                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10. 참고 자료

- [AWS S3 Pre-signed URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html)
- [AWS SDK for Java 2.x - Presigned URLs](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html)
- [S3 Transfer Acceleration](https://docs.aws.amazon.com/AmazonS3/latest/userguide/transfer-acceleration.html)
- [AWS Lambda Triggers for S3](https://docs.aws.amazon.com/lambda/latest/dg/with-s3.html)

---

*문서 작성일: 2026-01-29*
*작성자: Claude Code*
