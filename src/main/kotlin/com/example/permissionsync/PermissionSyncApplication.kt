package com.example.permissionsync

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PermissionSyncApplication

fun main(args: Array<String>) {
    runApplication<PermissionSyncApplication>(*args)
}
