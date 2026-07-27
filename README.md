# sb10-mopl-team3

[![codecov](https://codecov.io/gh/sb10-part4-team3/sb10-mopl-team3/graph/badge.svg?token=SRVN4SVY18)](https://codecov.io/gh/sb10-part4-team3/sb10-mopl-team3)

# MoPl — 3팀

> 영화·TV·스포츠 콘텐츠를 함께 즐기고 소통하는 콘텐츠 큐레이션 플랫폼

## 🔗 팀 협업 문서

[Notion 협업 페이지](https://app.notion.com/p/10-3-3830ccdce8a28024a088dfa5a213ada6?source=copy_link)

## 👥 팀원 구성

| 이름 | GitHub |
| --- | --- |
| 김현민 | [@Gusals911](https://github.com/Gusals911) |
| 안승리 | [@Atory0206](https://github.com/Atory0206) |
| 황민재 | [@rorm0819](https://github.com/rorm0819) |
| 최현호 | [@CHH01](https://github.com/CHH01) |

## 📌 프로젝트 소개

**콘텐츠 큐레이션 및 실시간 소셜 플랫폼의 Spring 백엔드 시스템 구축**

- **프로젝트 기간:** 2026.06.18 ~ 2026.07.29
## 🛠 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.4.0 |
| Data | Spring Data JPA, QueryDSL, PostgreSQL |
| Security | Spring Security, JWT, OAuth 2.0, CSRF |
| Batch & Cache | Spring Batch, Redis |
| Messaging & Realtime | Apache Kafka, WebSocket, SSE |
| External API | TMDB API, The Sports DB API |
| Cloud & Infrastructure | AWS ECS, AWS ECR, AWS RDS, AWS ElastiCache, AWS S3 |
| Managed Messaging | Confluent Cloud |
| API Documentation | Springdoc OpenAPI, Swagger UI |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus, Grafana |
| Test | JUnit 5, Mockito, Testcontainers, WireMock, JaCoCo |
| Build & Container | Gradle, Docker, Docker Compose |
| Collaboration & CI/CD | Git, GitHub, GitHub Actions, Discord |

## 👨‍💻 팀원별 구현 기능

### 김현민 — 사용자 · 인증 · 관리자

- 회원가입 및 로그인 기능 구현
- JWT 기반 Access Token·Refresh Token 인증 처리
- CSRF 방어 및 인증 세션 관리
- 사용자 강제 로그아웃 기능 구현
- 관리자 기반 사용자 권한 변경 및 계정 잠금 처리
- 임시 비밀번호 발급 및 이메일 전송 기능 구현
- OAuth 2.0 기반 소셜 로그인 고도화

### 안승리 — 콘텐츠 · 외부 API 수집 · 배치

- 콘텐츠 등록·조회·수정·삭제 기능 구현
- 콘텐츠 검색 및 필터링 기능 구현
- TMDB API 연동을 통한 영화·TV 콘텐츠 수집
- The Sports DB API 연동을 통한 스포츠 콘텐츠 수집
- Spring Batch 기반 외부 콘텐츠 정기 수집 작업 구현
- 콘텐츠 통계 및 검색 기반 구조 구현

### 황민재 — 큐레이션 · 프로필 · 소셜

- 콘텐츠 평점 및 의견 작성·조회·수정·삭제 기능 구현
- 개인 플레이리스트 생성 및 관리 기능 구현
- 플레이리스트 구독 기능 구현
- 사용자 프로필 조회 및 수정 기능 구현
- 사용자 팔로우·언팔로우 기능 구현
- 사용자 취향 기반 콘텐츠 큐레이션 기능 구현

### 최현호 — 실시간 통신 · 알림 · 메시징

- WebSocket 기반 실시간 통신 환경 구축
- 콘텐츠 시청 세션 생성 및 접속 상태 관리
- 콘텐츠별 실시간 채팅 기능 구현
- 사용자 간 1:1 다이렉트 메시지(DM) 기능 구현
- SSE 기반 실시간 알림 구독 및 전송 기능 구현
- DM 및 알림 이벤트 처리
- Kafka 기반 알림 메시징 및 이벤트 처리 구조 구현

## 📁 프로젝트 구조

```text
sb10-mopl-team3
├── src
│   ├── main
│   │   ├── java/com/example/sb10_MoPl_team3
│   │   │   ├── auth              # 로그인, JWT 인증, CSRF, 비밀번호 재설정
│   │   │   ├── oauth             # OAuth 2.0 소셜 로그인
│   │   │   ├── user              # 사용자 및 관리자 기능
│   │   │   ├── content           # 콘텐츠 CRUD, 검색 및 통계
│   │   │   ├── tmdb              # TMDB API 연동 및 콘텐츠 수집
│   │   │   ├── sportsdb          # The Sports DB API 연동 및 콘텐츠 수집
│   │   │   ├── batch             # 외부 콘텐츠 수집 스케줄링
│   │   │   ├── review            # 콘텐츠 평점 및 의견
│   │   │   ├── playlist          # 플레이리스트 및 구독
│   │   │   ├── follow            # 사용자 팔로우
│   │   │   ├── watchingsession   # 콘텐츠 시청 세션
│   │   │   ├── contentchat       # 콘텐츠 실시간 채팅
│   │   │   ├── conversation      # DM 대화방
│   │   │   ├── directmessage     # 1:1 다이렉트 메시지
│   │   │   ├── notification      # Kafka·SSE 기반 실시간 알림
│   │   │   ├── global
│   │   │   │   ├── config        # 공통 설정
│   │   │   │   ├── security      # Spring Security, JWT, CSRF
│   │   │   │   ├── websocket     # WebSocket 공통 설정
│   │   │   │   ├── sse           # SSE 연결 및 이벤트 관리
│   │   │   │   ├── exception     # 전역 예외 처리
│   │   │   │   ├── file          # AWS S3 파일 저장
│   │   │   │   ├── cursor        # 커서 기반 페이지네이션
│   │   │   │   └── metrics       # 애플리케이션 메트릭
│   │   │   ├── loadtest          # 부하 테스트용 데이터 구성
│   │   │   └── Sb10MoPlTeam3Application.java
│   │   │
│   │   └── resources
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       ├── application-prod.yaml
│   │       ├── application-oauth.yaml
│   │       └── static             # 프론트엔드 정적 리소스
│   │
│   └── test                       # 단위·통합 테스트
│
├── monitoring
│   ├── prometheus                 # Prometheus 수집 설정
│   └── grafana                    # Grafana 데이터소스 및 대시보드
│
├── .github
│   ├── workflows                  # GitHub Actions CI/CD
│   ├── ISSUE_TEMPLATE
│   └── pull_request_template.md
│
├── Dockerfile                     # 애플리케이션 이미지 빌드
├── docker-compose.yml             # 로컬 인프라 실행 환경
├── build.gradle                   # Gradle 의존성 및 빌드 설정
├── settings.gradle
└── README.md
```

## 🌐 구현 홈페이지

[MoPl 서비스 바로가기](https://moduplaylist.site)

## 📖 프로젝트 회고록

> 발표 자료 또는 프로젝트 회고록 링크가 추가될 예정입니다.
