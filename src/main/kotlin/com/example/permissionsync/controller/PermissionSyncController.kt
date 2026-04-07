package com.example.permissionsync.controller

import com.example.permissionsync.service.EmployeeChangeDetector
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/permission-sync")
class PermissionSyncController(
    private val employeeChangeDetector: EmployeeChangeDetector
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 스케줄러와 동일한 서비스 로직을 수동으로 즉시 실행하는 엔드포인트
    @PostMapping("/run")
    fun run(): ResponseEntity<String> {
        log.info("Manual permission sync triggered via API")
        val changed = employeeChangeDetector.detectChanges()
        return ResponseEntity.ok("Sync completed. Changed rows: ${changed.size}")
    }
}
