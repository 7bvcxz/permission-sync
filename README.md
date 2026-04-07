# Permission Sync

## 프로젝트 개요

`dbo.IF_DIMS_FOR_SECRTY` 테이블을 주기적으로 읽어 변경된 직원 정보를 감지하고,
보안 등급(`SECRTY_GRADE`) 조건에 따라 외부 API를 호출하는 Spring Boot 배치 서비스입니다.

| 항목 | 내용 |
|---|---|
| 실행 주기 | 20분마다 자동 실행 |
| 변경 감지 기준 | `EMPLY_NO` 기준으로 `SECRTY_GRADE` 값이 바뀐 행만 처리 |
| 조건 분기 | `SECRTY_GRADE` 2번째 글자 숫자 > 2 → CreateUser / > 7 → GrantPoweruser 호출 |
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

두 조건은 독립적으로 평가됩니다. 2번째 글자가 8 또는 9이면 두 API 모두 호출됩니다.

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
│   └── EmployeeChangeDetector.kt    # 핵심 로직: DB 조회 + 변경 감지 + 조건 분기
├── repository/
│   └── EmployeeRepository.kt        # JPA Repository, DB 읽기 전용
├── model/
│   └── Employee.kt                  # dbo.IF_DIMS_FOR_SECRTY 테이블 매핑 엔티티
├── controller/
│   └── PermissionSyncController.kt  # 수동 실행용 REST 엔드포인트
└── client/
    └── UserApiClient.kt             # 외부 REST API 호출 (CreateUser / GrantPoweruser)
```

---

## 설정 파일 구조

```
src/main/resources/
├── application.yml          # ✅ Git 커밋 O — 공통 설정 (JPA, Jasypt 암호화 설정)
├── application-dev.yml      # ✅ Git 커밋 O — DEV 환경 (암호화된 값 포함)
├── application-qa.yml       # ✅ Git 커밋 O — QA 환경 (암호화된 값 포함)
├── application-prod.yml     # ✅ Git 커밋 O — PROD 환경 (암호화된 값 포함)
└── application-local.yml    # ❌ Git 커밋 X — 로컬 전용, 직접 생성
```

### application-local.yml 만 Git에서 제외하는 이유
- `dev / qa / prod` 파일은 민감값이 **암호화된 상태**(`ENC(...)`)로 저장되므로 Git에 올려도 안전합니다.
- `application-local.yml`은 개발자 편의를 위해 평문으로 작성하는 로컬 전용 파일이므로 제외합니다.

---

## 민감정보 암호화 방식 (Jasypt)

이 프로젝트는 **Jasypt**를 사용해 DB 비밀번호 등 민감한 값을 암호화합니다.

### 동작 방식

```
설정 파일에 저장된 값:  ENC(암호화된문자열)
        ↓
앱 시작 시 Jasypt가 자동으로 복호화
        ↓
Spring Boot가 실제 비밀번호로 DB 연결
```

### 복호화 키 (APP_SECRET_KEY)

복호화 키는 **절대 코드나 Repository에 포함되지 않습니다.**
실행 환경에서 환경변수로 주입해야 합니다.

```bash
# 환경변수 설정
export APP_SECRET_KEY=팀에서-공유한-실제-키
```

---

## 실행 방법

### 로컬 환경 (application-local.yml 사용)

로컬 파일은 평문으로 작성하므로 APP_SECRET_KEY 값은 아무거나 넣어도 됩니다.

```bash
# 1. 로컬 설정 파일 생성 (최초 1회)
cp src/main/resources/application-dev.yml \
   src/main/resources/application-local.yml
# → 열어서 DB 주소, username, password 를 로컬 실제 값으로 수정 (ENC 제거)

# 2. 빌드
mvn clean package -DskipTests

# 3. 실행
export APP_SECRET_KEY=local-any-value
java -jar target/permission-sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### DEV / QA / PROD 환경

```bash
# DEV 실행
export APP_SECRET_KEY=팀에서-공유한-실제-키
java -jar target/permission-sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# QA 실행
export APP_SECRET_KEY=팀에서-공유한-실제-키
java -jar target/permission-sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=qa

# PROD 실행
export APP_SECRET_KEY=팀에서-공유한-실제-키
java -jar target/permission-sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Maven으로 바로 실행할 때

```bash
APP_SECRET_KEY=팀에서-공유한-실제-키 mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 암호화 값 생성 방법 (비밀번호 변경 시)

> ⚠️ DB 비밀번호가 변경되면 기존 `ENC(...)` 값은 더 이상 유효하지 않습니다.
> 새 비밀번호를 암호화해서 설정 파일에 다시 반영해야 합니다.

### 단계별 절차

**1단계 — 새 비밀번호를 암호화합니다**

```bash
# APP_SECRET_KEY 는 현재 팀에서 사용 중인 키를 입력
mvn jasypt:encrypt-value \
  -Djasypt.encryptor.password="APP_SECRET_KEY_값" \
  -Djasypt.plugin.value="새_비밀번호"
```

출력 예시:
```
ENC(aBcDeFgHiJkLmNoPqRsTuVwXyZ...)
```

**2단계 — 설정 파일에 반영합니다**

해당 환경의 yml 파일을 열어 기존 `ENC(...)` 값을 새 값으로 교체합니다.

```yaml
# application-prod.yml 예시
spring:
  datasource:
    password: ENC(aBcDeFgHiJkLmNoPqRsTuVwXyZ...)  # 여기를 교체
```

**3단계 — 커밋하고 배포합니다**

```bash
git add src/main/resources/application-prod.yml
git commit -m "Update encrypted DB password for prod"
git push
```

---

## APP_SECRET_KEY 공유 방법

### 키를 Repository에 올리지 않는 이유
`APP_SECRET_KEY`는 암호화된 값을 풀 수 있는 열쇠입니다.
이 키가 노출되면 설정 파일의 모든 암호화 값이 해독될 수 있으므로
**절대 코드나 Repository에 포함하면 안 됩니다.**

### 팀원에게 키를 전달하는 방법

복잡한 시스템 없이 현실적으로 쓸 수 있는 방법들입니다.

| 방법 | 설명 |
|---|---|
| **팀 메신저 DM** | Slack, Teams 등에서 1:1로 전달. 채널에 올리지 말 것 |
| **사내 비밀번호 관리 도구** | 1Password, Bitwarden 등 팀 vault 사용 |
| **전화/구두** | 보안이 중요한 경우 직접 전달 |

> 이메일 본문, GitHub 이슈, PR 코멘트, 공개 채널에는 절대 올리지 마세요.

### 로컬 환경에서 등록하는 방법

```bash
# 터미널 세션에 임시 등록 (세션 종료 시 사라짐)
export APP_SECRET_KEY=팀에서-받은-키

# 영구 등록 (~/.bashrc 또는 ~/.zshrc 에 추가)
echo 'export APP_SECRET_KEY=팀에서-받은-키' >> ~/.zshrc
source ~/.zshrc
```

### 서버/배포 환경에서 등록하는 방법

```bash
# systemd 서비스 파일의 [Service] 섹션에 추가
Environment="APP_SECRET_KEY=팀에서-받은-키"

# Docker 실행 시
docker run -e APP_SECRET_KEY=팀에서-받은-키 permission-sync

# Kubernetes Secret 사용
kubectl create secret generic app-secret --from-literal=APP_SECRET_KEY=팀에서-받은-키
# → Deployment 의 env 에서 참조
```

---

## API

### 수동 동기화 실행

```
POST /permission-sync/run
```

```bash
curl -X POST http://localhost:8080/permission-sync/run
```

응답 예시:
```
Sync completed. Changed rows: 3
```

---

## 주의사항

- **`APP_SECRET_KEY`는 절대 코드나 Repository에 넣지 마세요.**
  키가 노출되면 설정 파일의 암호화 값이 모두 해독됩니다.

- **`application-local.yml`은 절대 커밋하지 마세요.**
  `.gitignore`에 등록되어 있지만, `git add -f` 등으로 강제 추가하지 않도록 주의하세요.

- **DB 비밀번호 변경 시 반드시 재암호화가 필요합니다.**
  기존 `ENC(...)` 값은 이전 비밀번호 기준이므로, 새 비밀번호로 다시 암호화해 반영해야 합니다.

- **앱 재시작 시 변경 감지 상태가 초기화됩니다.**
  인메모리 방식이므로 재시작 직후 첫 실행에서는 모든 직원이 변경된 것으로 간주됩니다.

- **`UserApiClient`의 실제 HTTP 호출은 아직 구현되지 않았습니다.**
  `createUser()` / `grantPoweruser()` 내부에 TODO 주석이 있으며,
  `RestTemplate` 또는 `WebClient`로 구현 예정입니다.
