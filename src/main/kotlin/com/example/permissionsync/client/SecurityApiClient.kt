package com.example.permissionsync.client

import com.example.permissionsync.model.Employee
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Client for the external Security REST API.
 *
 * NOTE: createUser() and grantPoweruser() are NOT database write operations.
 * They are placeholders for outbound REST API calls to an external security
 * system. The actual HTTP wiring (RestTemplate / WebClient, endpoint URLs,
 * auth, payload schema) will be filled in later.
 */
@Component
class SecurityApiClient {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Calls the external Security API to create a user account
     * for the given employee. (REST call — no DB write.)
     */
    fun createUser(employee: Employee) {
        log.debug("[REST] POST createUser — EMPLY_NO={}", employee.emplyNo)
        // TODO: invoke external REST API: POST {securityApi}/users
    }

    /**
     * Calls the external Security API to grant power-user privileges
     * to the given employee. (REST call — no DB write.)
     */
    fun grantPoweruser(employee: Employee) {
        log.debug("[REST] POST grantPoweruser — EMPLY_NO={}", employee.emplyNo)
        // TODO: invoke external REST API: POST {securityApi}/users/{id}/poweruser
    }
}
