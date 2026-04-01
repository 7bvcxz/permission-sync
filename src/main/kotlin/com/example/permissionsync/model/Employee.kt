package com.example.permissionsync.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "TABLE_A")
data class Employee(
    @Id
    @Column(name = "EMPLY_NO", nullable = false)
    val emplyNo: String = "",

    @Column(name = "SECRTY_GRADE", nullable = false)
    val secrtyGrade: String = "",

    @Column(name = "IF_DATE", nullable = false)
    val ifDate: LocalDateTime = LocalDateTime.MIN
)
