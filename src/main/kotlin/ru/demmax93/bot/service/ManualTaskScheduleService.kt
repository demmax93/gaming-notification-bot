package ru.demmax93.bot.service

import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

@Service
class ManualTaskScheduleService(val scheduler: TaskScheduler) {
    val futures: MutableMap<Int, ScheduledFuture<*>> = HashMap()
    val taskId = AtomicInteger()

    fun addNewTask(task: Runnable, startDate: Date): Int {
        val scheduledTaskFuture = scheduler.schedule(task, startDate)
        val id = taskId.incrementAndGet()
        futures[id] = scheduledTaskFuture
        return id
    }

    fun removeTaskFromScheduler(id: Int) {
        futures[id]?.let {
            it.cancel(true)
            futures.remove(id)
        }
    }
}