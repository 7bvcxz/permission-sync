package com.example.permissionsync.service

import com.example.permissionsync.client.SecurityApiClient
import com.example.permissionsync.model.Employee
import com.example.permissionsync.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EmployeeChangeDetector(
    private val employeeRepository: EmployeeRepository,
    private val securityApiClient: SecurityApiClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // In-memory snapshot of the last known SECRTY_GRADE per employee, keyed by EMPLY_NO.
    // Populated on first run; updated after every run.
    // Limitation: cleared on application restart — all rows are treated as changed on first run.
    private val snapshot: MutableMap<String, String> = mutableMapOf()

    fun detectChanges(): List<Employee> {
        log.debug("Reading all employees from DB (dbo.IF_DIMS_FOR_SECRTY)")
        val current = try {
            employeeRepository.findAll()
        } catch (e: Exception) {
            log.error("Failed to read employee data from DB: {}", e.message, e)
            return emptyList()
        }
        log.info("DB read complete: {} employee row(s) loaded", current.size)

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

        // Update snapshot with current state after processing
        current.forEach { emp -> snapshot[emp.emplyNo] = emp.secrtyGrade }

        return changed
    }

    private fun applyGradeConditions(employee: Employee) {
        val raw = employee.secrtyGrade
        val gradeDigit = raw.getOrNull(1)?.digitToIntOrNull()

        if (gradeDigit == null) {
            log.warn("EMPLY_NO={} has invalid or short SECRTY_GRADE '{}' — skipping condition evaluation", employee.emplyNo, raw)
            return
        }

        log.debug("EMPLY_NO={} SECRTY_GRADE='{}' grade digit={}", employee.emplyNo, raw, gradeDigit)

        // CreateUser and GrantPoweruser are external REST API calls (see SecurityApiClient),
        // not DB writes. Each call is isolated so a failure of one does not block the other.
        if (gradeDigit > 2) {
            log.info("EMPLY_NO={} grade digit {} > 2 — calling CreateUser (REST)", employee.emplyNo, gradeDigit)
            try {
                securityApiClient.createUser(employee)
                log.debug("CreateUser completed for EMPLY_NO={}", employee.emplyNo)
            } catch (e: Exception) {
                log.error("CreateUser failed for EMPLY_NO={}: {}", employee.emplyNo, e.message, e)
            }
        }

        if (gradeDigit > 7) {
            log.info("EMPLY_NO={} grade digit {} > 7 — calling GrantPoweruser (REST)", employee.emplyNo, gradeDigit)
            try {
                securityApiClient.grantPoweruser(employee)
                log.debug("GrantPoweruser completed for EMPLY_NO={}", employee.emplyNo)
            } catch (e: Exception) {
                log.error("GrantPoweruser failed for EMPLY_NO={}: {}", employee.emplyNo, e.message, e)
            }
        }
    }
}
