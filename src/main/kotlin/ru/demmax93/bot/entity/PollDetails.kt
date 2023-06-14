package ru.demmax93.bot.entity

import java.time.LocalDateTime

data class PollDetails(
    var messageId: Int,
    var scheduledTime: LocalDateTime?,
    var taskId: Int
)
