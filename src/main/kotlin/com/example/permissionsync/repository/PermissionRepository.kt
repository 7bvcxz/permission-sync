package com.example.permissionsync.repository

import com.example.permissionsync.model.Permission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PermissionRepository : JpaRepository<Permission, Long> {
    fun findByIsActiveTrue(): List<Permission>
    fun findByResource(resource: String): List<Permission>
}
