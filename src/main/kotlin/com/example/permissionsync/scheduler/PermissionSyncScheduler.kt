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

    @Scheduled(fixedRate = 20 * 60 * 1000) // every 20 minutes
    fun syncPermissions() {
        log.info("=== Permission sync started ===")
        try {
            employeeChangeDetector.detectChanges()
            log.info("=== Permission sync completed successfully ===")
        } catch (e: Exception) {
            log.error("Permission sync failed with exception: {}", e.message, e)
        }
    }
}
