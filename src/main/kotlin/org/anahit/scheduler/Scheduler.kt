package org.anahit.scheduler

import kotlinx.coroutines.*
import org.anahit.api.ApiTaskResult
import org.anahit.config.TaskConfig
import org.anahit.logging.Logger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Scheduler for API tasks.
 * This class is responsible for scheduling and executing tasks according to their cron expressions.
 */
class Scheduler(
    private val taskConfigs: List<TaskConfig>,
    private val checkInterval: Long = 1000, // Check for tasks to run every second by default
) {
    private val logger = Logger.getLogger(this::class.java.name)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val runningTasks = ConcurrentHashMap<String, Job>()
    private val taskRetries = ConcurrentHashMap<String, Int>()
    private var schedulerJob: Job? = null
    private var isRunning = false

    /**
     * Start the scheduler.
     */
    fun start() {
        if (isRunning) {
            logger.warn("Scheduler Is Already Running, Skipping Start")
            return
        }

        // Validate all task configs
        taskConfigs.forEach { it.validate() }
        logger.info(
            "Tasks Validated & Scheduled: ${
                taskConfigs
                    .joinToString(", ") { it.task.taskName }
            }",
        )

        isRunning = true
        schedulerJob =
            scope.launch {
                while (isActive) {
                    checkAndRunTasks()
                    delay(checkInterval)
                }
            }

        logger.info("Scheduler Started Successfully At ${LocalDateTime.now()}")
    }

    /**
     * Check for tasks that need to be run and execute them.
     */
    private fun checkAndRunTasks() {
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        taskConfigs.forEach { config ->
            val taskName = config.task.taskName

            // Skip if a task is already running
            if (runningTasks.containsKey(taskName) && runningTasks[taskName]?.isActive == true) {
                return@forEach
            }

            // Check if the task should run now
            if (config.shouldRunAt(now)) {
                val job =
                    scope.launch {
                        try {
                            config.task.saveApiTask(config)
                            // Reset retry count for new execution
                            taskRetries[taskName] = 0

                            // Execute the task with timeout
                            val result: Pair<ApiTaskResult, MutableList<Any>> =
                                withTimeoutOrNull(config.timeout.toMillis()) {
                                    config.task.execute()
                                } ?: Pair(
                                    ApiTaskResult.Error(
                                        taskId = config.taskId,
                                        apiTaskRunName = "$today-${config.taskName}",
                                        apiTaskRunStatus = "Failed",
                                        apiTaskRunExecutedAt = now,
                                    ),
                                    mutableListOf(),
                                )

                            logger.info("Task $taskName Completed Successfully")
                            config.task.saveApiTaskRun(result)
                            config.task.saveApiResults(result)
                        } catch (e: Exception) {
                            logger.error("Unexpected Error Executing Task $taskName", e)
                            logger.error("Task $taskName Failed")
                        } finally {
                            runningTasks.remove(taskName)
                        }
                    }
                runningTasks[taskName] = job
            }
        }
    }
}
