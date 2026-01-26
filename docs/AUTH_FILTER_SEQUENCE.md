# 인증 필터 및 로그인 프로세스 시퀀스 다이어그램

이 문서는 MeeTeam 백엔드의 인증 처리 과정, 특히 필터 간의 흐름과 로그인 시 내부 동작 원리를 시각적으로 설명합니다.

## 1. 전체 필터 체인 흐름 (로그인 요청 시)

사용자가 `POST /api/auth/login` 요청을 보냈을 때, 요청이 어떤 필터들을 거쳐서 처리되는지 보여줍니다.

```mermaid
sequenceDiagram
    actor Client as 클라이언트 (User)
    participant JwtFilter
    participant LoginFilter
    participant AuthManager as AuthenticationManager
    participant Controller as (기타 필터들/Controller)

    Note over Client, Controller: 사용자가 로그인 요청 (POST /api/auth/login)

    Client->>JwtFilter: 1. 요청 도착
    JwtFilter->>JwtFilter: 토큰 확인 (로그인 요청이라 없음)
    JwtFilter->>LoginFilter: 2. doFilter() 호출 (다음 필터로 전달)
    
    Note over LoginFilter: 3. URL 확인 (/api/auth/login 일치!)
    
    LoginFilter->>LoginFilter: attemptAuthentication() 실행
    LoginFilter->>LoginFilter: JSON 파싱 (ID/PW 추출)
    
    LoginFilter->>AuthManager: 4. authenticate(authToken) 호출 (인증 의뢰)
    
    alt 인증 성공
        AuthManager-->>LoginFilter: 인증 성공 (Authentication 객체 반환)
        LoginFilter->>LoginFilter: successfulAuthentication() 실행
        LoginFilter-->>Client: 5. AccessToken + RefreshToken 응답
    else 인증 실패
        AuthManager-->>LoginFilter: 예외 발생 (비밀번호 틀림 등)
        LoginFilter->>LoginFilter: unsuccessfulAuthentication() 실행
        LoginFilter-->>Client: 401 Unauthorized 응답
    end

    Note over LoginFilter, Controller: 로그인 요청은 여기서 처리가 끝나므로<br/>다음 필터나 컨트롤러로 넘어가지 않음
```

---

## 2. 로그인 상세 프로세스 (내부 동작)

`LoginFilter`가 `AuthenticationManager`에게 인증을 의뢰했을 때, 내부적으로 DB 조회가 어떻게 일어나는지 보여줍니다.

```mermaid
sequenceDiagram
    participant LoginFilter
    participant AuthManager as AuthenticationManager
    participant AuthProvider as DaoAuthenticationProvider
    participant UserDetailService as CustomUserDetailsService
    participant DB as 데이터베이스

    Note over LoginFilter: attemptAuthentication() 내부

    LoginFilter->>AuthManager: authenticate(아이디, 비번)
    
    Note right of AuthManager: 실제 인증을 담당할 Provider 찾음
    AuthManager->>AuthProvider: 인증 처리 요청
    
    Note right of AuthProvider: "유저 정보가 필요해!"
    AuthProvider->>UserDetailService: loadUserByUsername(아이디)
    
    UserDetailService->>DB: findByEmail(아이디)
    DB-->>UserDetailService: Member 엔티티 반환
    
    UserDetailService-->>AuthProvider: UserDetails (유저 정보 + 암호화된 비번)
    
    Note right of AuthProvider: [자동 수행]<br/>입력된 비번 vs DB 비번 비교
    
    alt 비밀번호 일치
        AuthProvider-->>AuthManager: 인증 성공 (Authentication)
        AuthManager-->>LoginFilter: 인증 성공 (Authentication)
    else 불일치
        AuthProvider-->>AuthManager: BadCredentialsException 발생
        AuthManager-->>LoginFilter: 예외 전파
    end
```

## 요약

1.  **JwtFilter**: 모든 요청의 문지기. 로그인 요청은 그냥 통과시킵니다.
2.  **LoginFilter**: `/api/auth/login` 요청만 낚아채서 처리합니다.
3.  **AuthenticationManager**: 인증 총괄 매니저입니다. 실제 일은 `Provider`에게 시킵니다.
4.  **CustomUserDetailsService**: 우리가 직접 구현한 부분입니다. **DB에서 유저 정보를 꺼내오는 역할**만 합니다.
5.  **비밀번호 검증**: Spring Security가 내부적으로 알아서 수행하므로 우리가 코드를 짤 필요가 없습니다.
