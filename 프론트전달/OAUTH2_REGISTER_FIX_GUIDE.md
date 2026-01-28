# OAuth2 회원가입 API 에러 원인 및 조치 가이드

## 1. 문제 상황 (Issue)
**발생 오류:** `500 Internal Server Error (IllegalArgumentException: CharSequence cannot be null or empty)`
**발생 시점:** OAuth2 회원가입 (`POST /api/auth/register/oauth2`) 요청 시
**원인:**
- 프론트엔드에서 API 요청 시, **JSON Body(`request` 파트)에 `token` 필드가 누락됨**.
- 백엔드 DTO(`OAuth2RegisterRequest`)는 `token` 필드를 필수로 기대했으나, 값이 없어 JWT 파싱 과정에서 에러 발생.

---

## 2. 백엔드 조치 사항 (Backend Fix)
프론트엔드의 수정 부담을 줄이고 안정성을 높이기 위해 백엔드 로직을 개선했습니다.

1.  **Fallback 로직 추가:** JSON Body에 `token`이 없으면, 자동으로 **`Authorization` 헤더의 Bearer Token**을 읽어서 처리하도록 수정했습니다.
2.  **에러 응답 개선:** 만약 Body와 Header 모두 토큰이 없다면, `500 Server Error` 대신 명확한 `400 Bad Request` 에러를 반환합니다.

> **현재 상태:** 프론트엔드 수정 없이도, **헤더에 토큰을 잘 실어 보내고 있다면** API가 정상 동작할 것입니다.

---

## 3. 프론트엔드 권장 수정 사항 (Recommended Action)
백엔드에서 예외 처리를 추가했지만, API 명세(DTO)를 준수하는 것이 가장 안전합니다. 추후 명확한 데이터 전달을 위해 아래와 같이 **JSON Body에도 `token`을 포함**시켜 주시는 것을 권장합니다.

### ✅ 권장하는 요청 방식 (Multipart/form-data)

**Endpoint:** `POST /api/auth/register/oauth2`
**Header:** `Content-Type: multipart/form-data` (브라우저 자동 설정)

#### Javascript (Axios/Fetch) 예시

```javascript
const formData = new FormData();

// 1. JSON 데이터 (request)
const requestData = {
    // [중요] token 필드를 명시적으로 포함해주세요!
    token: "eyJhGciO...", // 회원가입용 토큰 (URL 파라미터로 받은 것)
    
    name: "사용자이름",
    age: 25,
    gender: "MALE",
    subCategories: [ { subcategory: "백엔드" } ],
    skills: [ { skillName: "Spring" } ]
};

// JSON을 문자열로 변환하여 FormData에 추가
const jsonBlob = new Blob([JSON.stringify(requestData)], { type: "application/json" });
formData.append("request", jsonBlob);

// 2. 프로필 이미지 (선택)
if (file) {
    formData.append("file", file);
}

// 3. API 전송
await axios.post('/api/auth/register/oauth2', formData, {
    headers: {
        // [선택] 백엔드 패치로 인해 헤더에만 있어도 동작은 하지만, 
        // Body에도 넣어주는 것이 정석입니다.
        Authorization: `Bearer ${token}` 
    }
});
```

### 요약
1. **백엔드 패치 완료:** 현재 상태에서도 작동할 가능성이 높음 (헤더 체크 로직 추가됨).
2. **프론트 수정 권장:** 확실한 동작을 위해 `FormData`의 `request` JSON 객체 안에 `token` 필드를 꼭 포함시켜 주세요.
