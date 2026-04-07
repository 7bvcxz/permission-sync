package com.example.permissionsync.scheduler

import com.example.permissionsync.service.EmployeeChangeDetector
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PermissionSyncScheduler(
    private val employeeChangeDetector: EmployeeChangeDetector
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 20분마다 자동 실행되는 스케줄러 진입점
    @Scheduled(fixedRate = 20 * 60 * 1000)
    fun syncPermissions() {
        log.info("=== Permission sync started ===")
        try {
            // 변경된 직원 조회 및 조건 처리 위임
            employeeChangeDetector.detectChanges()
            log.info("=== Permission sync completed successfully ===")
        } catch (e: Exception) {
            log.error("Permission sync failed with exception: {}", e.message, e)
        }
    }
}
