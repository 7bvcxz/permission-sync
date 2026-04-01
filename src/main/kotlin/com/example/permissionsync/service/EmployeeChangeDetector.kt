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

        val changed = if (since == null) {
            log.info("First run — loading all employees as baseline")
            employeeRepository.findAll()
        } else {
            log.info("Detecting employee changes since {}", since)
            employeeRepository.findByIfDateAfter(since)
        }

        log.info("Detected {} changed employee row(s)", changed.size)
        changed.forEach { emp ->
            log.debug("Changed: EMPLY_NO={} SECRTY_GRADE={} IF_DATE={}", emp.emplyNo, emp.secrtyGrade, emp.ifDate)
            applyGradeConditions(emp)
        }

        lastSyncedAt = checkpoint
        return changed
    }

    private fun applyGradeConditions(employee: Employee) {
        val gradeDigit = employee.secrtyGrade.getOrNull(1)?.digitToIntOrNull() ?: return

        if (gradeDigit > 2) functionX(employee)
        if (gradeDigit > 7) functionY(employee)
    }

    private fun functionX(employee: Employee) {
        // TODO: implement function X
    }

    private fun functionY(employee: Employee) {
        // TODO: implement function Y
    }
}
