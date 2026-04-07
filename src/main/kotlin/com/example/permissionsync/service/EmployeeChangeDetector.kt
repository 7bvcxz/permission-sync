package com.example.permissionsync.service

import com.example.permissionsync.client.UserApiClient
import com.example.permissionsync.model.Employee
import com.example.permissionsync.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EmployeeChangeDetector(
    private val employeeRepository: EmployeeRepository,
    private val userApiClient: UserApiClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 직전 실행 시점의 SECRTY_GRADE 값을 EMPLY_NO 기준으로 메모리에 보관
    // 앱 재시작 시 초기화되며, 첫 실행에서는 모든 행이 변경된 것으로 처리됨
    private val snapshot: MutableMap<String, String> = mutableMapOf()

    fun detectChanges(): List<Employee> {
        // dbo.IF_DIMS_FOR_SECRTY 전체 조회 (DB는 읽기 전용으로만 사용)
        val current = try {
            employeeRepository.findAll()
        } catch (e: Exception) {
            log.error("Failed to read employee data from DB: {}", e.message, e)
            return emptyList()
        }
        log.info("DB read complete: {} employee row(s) loaded", current.size)

        // snapshot과 비교하여 SECRTY_GRADE가 바뀐 행만 추출
        val changed = current.filter { emp ->
            val previous = snapshot[emp.emplyNo]
            previous == null || previous != emp.secrtyGrade
        }

        if (changed.isEmpty()) {
            log.info("No employee changes detected — skipping condition evaluation")
        } else {
            log.info("Detected {} changed employee row(s)", changed.size)
            changed.forEach { emp ->
                log.debug("Processing changed row: EMPLY_NO={} SECRTY_GRADE={} IF_DATE={}", emp.emplyNo, emp.secrtyGrade, emp.ifDate)
                applyGradeConditions(emp)
            }
        }

        // 처리 완료 후 현재 상태를 snapshot에 저장 (다음 실행의 비교 기준)
        current.forEach { emp -> snapshot[emp.emplyNo] = emp.secrtyGrade }

        return changed
    }

    private fun applyGradeConditions(employee: Employee) {
        val raw = employee.secrtyGrade
        // SECRTY_GRADE의 2번째 글자를 숫자로 변환 (없거나 숫자가 아니면 skip)
        val gradeDigit = raw.getOrNull(1)?.digitToIntOrNull()

        if (gradeDigit == null) {
            log.warn("EMPLY_NO={} has invalid or short SECRTY_GRADE '{}' — skipping condition evaluation", employee.emplyNo, raw)
            return
        }

        log.debug("EMPLY_NO={} SECRTY_GRADE='{}' grade digit={}", employee.emplyNo, raw, gradeDigit)

        // 외부 REST API 호출 (DB write 아님). 각각 독립적으로 try/catch 처리
        if (gradeDigit > 2) {
            // 2 초과 → 외부 API로 사용자 계정 생성 요청
            log.info("EMPLY_NO={} grade digit {} > 2 — calling CreateUser (REST)", employee.emplyNo, gradeDigit)
            try {
                userApiClient.createUser(employee)
                log.debug("CreateUser completed for EMPLY_NO={}", employee.emplyNo)
            } catch (e: Exception) {
                log.error("CreateUser failed for EMPLY_NO={}: {}", employee.emplyNo, e.message, e)
            }
        }

        if (gradeDigit > 7) {
            // 7 초과 → 외부 API로 파워유저 권한 부여 요청
            log.info("EMPLY_NO={} grade digit {} > 7 — calling GrantPoweruser (REST)", employee.emplyNo, gradeDigit)
            try {
                userApiClient.grantPoweruser(employee)
                log.debug("GrantPoweruser completed for EMPLY_NO={}", employee.emplyNo)
            } catch (e: Exception) {
                log.error("GrantPoweruser failed for EMPLY_NO={}: {}", employee.emplyNo, e.message, e)
            }
        }
    }
}
