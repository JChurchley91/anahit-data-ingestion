package org.anahit.config

import org.anahit.api.ApiTask
import org.quartz.CronExpression
import java.time.Duration
import java.time.LocalDateTime

/**
 * Configuration for a scheduled task.
 * This class contains all the configuration needed to schedule and execute a task.
 */
data class TaskConfig(
    /**
     * The API task to execute.
     */
    val task: ApiTask,
    val taskId: Int,
    val taskName: String,
    val taskDescription: String,
    /**
     * The cron expression for scheduling the task.
     * This follows the Quartz cron expression format.
     * Example: "0 0/15 * * * ?" (every 15 minutes)
     */
    val cronExpression: String,
    /**
     * Whether the task should be enabled.
     */
    val enabled: Boolean = true,
    /**
     * Maximum number of retries if the task fails.
     */
    val maxRetries: Int = 3,
    /**
     * Delay between retries.
     */
    val retryDelay: Duration = Duration.ofMinutes(5),
    /**
     * Timeout for the task execution.
     */
    val timeout: Duration = Duration.ofMinutes(10),
) {
    /**
     * Validates that the task configuration is valid.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    fun validate() {
        require(task.taskName.isNotBlank()) { "Task name cannot be empty" }
        require(CronExpression.isValidExpression(cronExpression)) { "Invalid cron expression: $cronExpression" }
        require(maxRetries >= 0) { "Max retries must be non-negative" }
        require(!retryDelay.isNegative) { "Retry delay must be non-negative" }
        require(!timeout.isNegative) { "Timeout must be non-negative" }
    }

    /**
     * Checks if the task should run at the given time.
     *
     * @param time The time to check
     * @return True if the task should run at the given time, false otherwise
     */
    fun shouldRunAt(time: LocalDateTime): Boolean {
        if (!enabled) return false

        val cronExpression = CronExpression(this.cronExpression)
        val date = java.util.Date.from(time.atZone(java.time.ZoneId.systemDefault()).toInstant())
        return cronExpression.isSatisfiedBy(date)
    }
}
