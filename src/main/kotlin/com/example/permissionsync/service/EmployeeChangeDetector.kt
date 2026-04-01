package com.example.permissionsync.service

import com.example.permissionsync.model.Employee
import com.example.permissionsync.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EmployeeChangeDetector(
    private val employeeRepository: EmployeeRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Tracks the IF_DATE watermark from the last successful detection run.
    // null on first run → loads all rows as the initial baseline.
    private var lastSyncedAt: LocalDateTime? = null

    fun detectChanges(): List<Employee> {
        val since = lastSyncedAt
        val checkpoint = LocalDateTime.now()

        val changed = try {
            if (since == null) {
                log.info("First run — reading all employees from DB as baseline")
                val rows = employeeRepository.findAll()
                log.info("Baseline load complete: {} employee row(s) read", rows.size)
                rows
            } else {
                log.debug("Reading changed employees from DB (IF_DATE after {})", since)
                val rows = employeeRepository.findByIfDateAfter(since)
                log.info("DB read complete: {} changed employee row(s) detected since {}", rows.size, since)
                rows
            }
        } catch (e: Exception) {
            log.error("Failed to read employee data from DB: {}", e.message, e)
            return emptyList()
        }

        if (changed.isEmpty()) {
            log.info("No employee changes detected — skipping condition evaluation")
        } else {
            changed.forEach { emp ->
                log.debug("Processing changed row: EMPLY_NO={} SECRTY_GRADE={} IF_DATE={}", emp.emplyNo, emp.secrtyGrade, emp.ifDate)
                applyGradeConditions(emp)
            }
        }

        lastSyncedAt = checkpoint
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

        if (gradeDigit > 2) {
            log.info("EMPLY_NO={} grade digit {} > 2 — calling functionX", employee.emplyNo, gradeDigit)
            try {
                functionX(employee)
                log.debug("functionX completed for EMPLY_NO={}", employee.emplyNo)
            } catch (e: Exception) {
                log.error("functionX failed for EMPLY_NO={}: {}", employee.emplyNo, e.message, e)
            }
        }

        if (gradeDigit > 7) {
            log.info("EMPLY_NO={} grade digit {} > 7 — calling functionY", employee.emplyNo, gradeDigit)
            try {
                functionY(employee)
                log.debug("functionY completed for EMPLY_NO={}", employee.emplyNo)
            } catch (e: Exception) {
                log.error("functionY failed for EMPLY_NO={}: {}", employee.emplyNo, e.message, e)
            }
        }
    }

    private fun functionX(employee: Employee) {
        // TODO: implement function X
    }

    private fun functionY(employee: Employee) {
        // TODO: implement function Y
    }
}
