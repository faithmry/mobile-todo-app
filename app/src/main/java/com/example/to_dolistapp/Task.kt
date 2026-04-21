package com.example.to_dolistapp

import java.time.LocalDateTime
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val deadline: LocalDateTime,
    val isCompleted: Boolean = false
)