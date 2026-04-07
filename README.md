# Permission Sync

## 프로젝트 개요

`dbo.IF_DIMS_FOR_SECRTY` 테이블을 주기적으로 읽어 변경된 직원 정보를 감지하고,
보안 등급(`SECRTY_GRADE`) 조건에 따라 외부 API를 호출하는 Spring Boot 배치 서비스입니다.

| 항목 | 내용 |
|---|---|
| 실행 주기 | 20분마다 자동 실행 |
| 변경 감지 기준 | `EMPLY_NO` 기준으로 `SECRTY_GRADE` 값이 바뀐 행만 처리 |
| 조건 분기 | `SECRTY_GRADE` 2번째 글자 숫자 기준 → CreateUser / GrantPoweruser 호출 |
| DB 역할 | 읽기 전용 (`dbo.IF_DIMS_FOR_SECRTY`) |
| 외부 호출 | REST API (DB write 없음) |

---

## 주요 기능

### 1. 스케줄러 자동 실행
20분마다 자동으로 권한 동기화 작업을 실행합니다.

### 2. 변경 감지 (인메모리 비교)
애플리케이션 메모리에 직전 실행 결과를 보관하고,
다음 실행 시 `SECRTY_GRADE` 값이 달라진 직원만 추출합니다.

> ⚠️ 앱 재시작 시 인메모리 상태가 초기화되어, 첫 실행에서는 모든 직원이 변경된 것으로 처리됩니다.

### 3. 보안 등급 조건 분기

| 조건 | 호출되는 API |
|---|---|
| `SECRTY_GRADE` 2번째 글자 > 2 | CreateUser |
| `SECRTY_GRADE` 2번째 글자 > 7 | GrantPoweruser |

두 조건은 독립적으로 평가되므로, 2번째 글자가 8 또는 9이면 두 API 모두 호출됩니다.

### 4. 수동 실행 API
스케줄러를 기다리지 않고 즉시 동기화를 실행할 수 있는 REST 엔드포인트를 제공합니다.

---

## 프로젝트 구조

```
src/main/kotlin/com/example/permissionsync/
├── PermissionSyncApplication.kt     # Spring Boot 시작점, 스케줄러 활성화
├── scheduler/
│   └── PermissionSyncScheduler.kt   # 20분마다 자동 실행 (@Scheduled)
├── service/
│   └── EmployeeChangeDetector.kt    # 핵심 비즈니스 로직: DB 조회 + 변경 감지 + 조건 분기
├── repository/
│   └── EmployeeRepository.kt        # JPA Repository, DB 읽기 전용
├── model/
│   └── Employee.kt                  # dbo.IF_DIMS_FOR_SECRTY 테이블 매핑 엔티티
├── controller/
│   └── PermissionSyncController.kt  # 수동 실행용 REST 엔드포인트
└── client/
    └── UserApiClient.kt             # 외부 REST API 호출 클라이언트 (CreateUser / GrantPoweruser)
```

---

## 실행 방법

### 사전 준비

처음 실행하는 경우, 환경별 설정 파일을 만들어야 합니다.
`application-example.yml`을 복사해서 본인 환경에 맞게 수정하세요.

```bash
# 로컬 개발용 설정 파일 생성
cp src/main/resources/application-example.yml \
   src/main/resources/application-local.yml

# 이후 application-local.yml 을 열어 DB 주소, 계정, API URL 을 실제 값으로 수정
```

### 빌드

```bash
mvn clean package
```

### 환경별 실행

```bash
# 로컬 개발 환경
java -jar target/permission-sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=local

# 개발 서버
java -jar target/permission-sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# QA 서버
java -jar target/permission-sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=qa

# 운영 서버
java -jar target/permission-sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Maven으로 바로 실행할 때

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 설정 파일 구조

```
src/main/resources/
├── application.yml          # ✅ Git 커밋 O — 모든 환경 공통 설정 (JPA 등)
├── application-example.yml  # ✅ Git 커밋 O — 설정 예시 파일 (실제 값 없음)
├── application-local.yml    # ❌ Git 커밋 X — 로컬 개발용, 직접 생성
├── application-dev.yml      # ❌ Git 커밋 X — 개발 서버용, 서버에서 직접 관리
├── application-qa.yml       # ❌ Git 커밋 X — QA 서버용, 서버에서 직접 관리
└── application-prod.yml     # ❌ Git 커밋 X — 운영 서버용, 서버에서 직접 관리
```

`application-local/dev/qa/prod.yml`은 `.gitignore`에 등록되어 있어 Git에 올라가지 않습니다.

### 각 파일에 들어가는 설정

**`application.yml`** (공통 — Git 관리)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

**`application-local.yml`** (로컬 전용 — Git 미포함, 직접 생성)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/permissionsdb_local
    username: postgres
    password: 실제_비밀번호
    driver-class-name: org.postgresql.Driver

user-api:
  base-url: http://localhost:8081
```

---

## API

### 수동 동기화 실행

스케줄러 주기(20분)를 기다리지 않고 즉시 실행할 때 사용합니다.

```
POST /permission-sync/run
```

```bash
curl -X POST http://localhost:8080/permission-sync/run
```

**응답 예시**
```
Sync completed. Changed rows: 3
```

---

## 주의사항

- **DB 계정, 비밀번호, API URL 등 민감한 값은 절대 Git에 커밋하지 마세요.**
  `application-local/dev/qa/prod.yml`은 `.gitignore`에 등록되어 있지만,
  실수로 다른 파일에 실제 값을 넣지 않도록 주의하세요.

- **앱 재시작 시 변경 감지 상태가 초기화됩니다.**
  인메모리 방식이므로 재시작 직후 첫 실행에서는 모든 직원이 변경된 것으로 간주됩니다.

- **`application-example.yml`을 직접 수정하지 마세요.**
  이 파일은 팀 전체가 참고하는 샘플 파일입니다. 복사 후 별도 파일로 사용하세요.

- **`UserApiClient`의 실제 HTTP 호출은 아직 구현되지 않았습니다.**
  `createUser()` / `grantPoweruser()` 내부에 TODO 주석이 있으며,
  `RestTemplate` 또는 `WebClient`로 구현 예정입니다.
