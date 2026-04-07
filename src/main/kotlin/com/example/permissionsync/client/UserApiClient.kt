package com.example.permissionsync.client

import com.example.permissionsync.model.Employee
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

// 외부 보안 시스템과 통신하는 REST API 클라이언트 (DB 접근 없음)
// 실제 HTTP 호출 구현 시 RestTemplate 또는 WebClient 사용 예정
@Component
class UserApiClient(
    // application-{profile}.yml 의 user-api.base-url 값이 자동으로 주입됨
    @Value("\${user-api.base-url}")
    private val baseUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 사용자 계정 생성 요청 — 외부 REST API 호출 (POST /users)
    fun createUser(employee: Employee) {
        val url = "$baseUrl/users"
        log.debug("[REST] POST createUser — url={} EMPLY_NO={}", url, employee.emplyNo)
        // TODO: invoke external REST API: POST $baseUrl/users
    }

    // 파워유저 권한 부여 요청 — 외부 REST API 호출 (POST /users/{id}/poweruser)
    fun grantPoweruser(employee: Employee) {
        val url = "$baseUrl/users/${employee.emplyNo}/poweruser"
        log.debug("[REST] POST grantPoweruser — url={} EMPLY_NO={}", url, employee.emplyNo)
        // TODO: invoke external REST API: POST $baseUrl/users/{id}/poweruser
    }
}
