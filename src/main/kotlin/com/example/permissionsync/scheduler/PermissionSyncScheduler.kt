package com.example.permissionsync.scheduler

import com.example.permissionsync.repository.PermissionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PermissionSyncScheduler(
    private val permissionRepository: PermissionRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 20 * 60 * 1000) // every 20 minutes
    fun syncPermissions() {
        log.info("Starting permission sync...")

        val permissions = permissionRepository.findByIsActiveTrue()
        log.info("Loaded {} active permissions from DB", permissions.size)

        // TODO: add your sync logic here
        permissions.forEach { permission ->
            log.debug("Syncing permission: {} -> {} on {}", permission.name, permission.action, permission.resource)
        }

        log.info("Permission sync complete.")
    }
}
