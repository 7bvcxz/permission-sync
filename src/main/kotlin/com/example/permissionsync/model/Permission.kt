package com.example.permissionsync.model

import jakarta.persistence.*

@Entity
@Table(name = "permissions")
data class Permission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val resource: String = "",

    @Column(nullable = false)
    val action: String = "",

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)
