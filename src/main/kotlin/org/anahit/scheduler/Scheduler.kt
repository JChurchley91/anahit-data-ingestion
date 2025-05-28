package org.anahit.scheduler

import kotlinx.coroutines.*
import org.anahit.api.ApiTaskResult
import org.anahit.config.TaskConfig
import org.anahit.logging.Logger
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * Scheduler for API tasks.
 * This class is responsible for scheduling and executing tasks according to their cron expressions.
 */
class Scheduler(
    private val taskConfigs: List<TaskConfig>,
    private val checkInterval: Long = 1000 // Check for tasks to run every second by default
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
        logger.info("Tasks Validated & Scheduled: ${taskConfigs
            .joinToString(", ") { it.task.name }}")
        
        isRunning = true
        schedulerJob = scope.launch {
            while (isActive) {
                checkAndRunTasks()
                delay(checkInterval)
            }
        }
        
        logger.info("Scheduler Started Successfully At ${LocalDateTime.now()}")
    }
    
    /**
     * Stop the scheduler.
     */
    fun stop() {
        if (!isRunning) {
            logger.warn("Scheduler Is Not Running")
            return
        }
        
        logger.info("Stopping Scheduler")
        schedulerJob?.cancel()
        runningTasks.values.forEach { it.cancel() }
        runningTasks.clear()
        taskRetries.clear()
        isRunning = false
        logger.info("Scheduler Stopped Successfully At ${LocalDateTime.now()}")
    }
    
    /**
     * Check for tasks that need to be run and execute them.
     */
    private fun checkAndRunTasks() {
        val now = LocalDateTime.now()
        
        taskConfigs.forEach { config ->
            val taskName = config.task.name
            val nextExecutionTime = config.getNextExecutionTime(now)
            
            // Skip if a task is already running
            if (runningTasks.containsKey(taskName) && runningTasks[taskName]?.isActive == true) {
                return@forEach
            }
            
            // Check if the task should run now
            if (config.shouldRunAt(now)) {
                val job = scope.launch {
                    try {
                        // Reset retry count for new execution
                        taskRetries[taskName] = 0
                        
                        // Execute the task with timeout
                        val result = withTimeoutOrNull(config.timeout.toMillis()) {
                            config.task.execute(config.parameters)
                        } ?: ApiTaskResult.Error(
                            taskName = taskName,
                            errorMessage = "Task Execution Timed Out After ${config.timeout.seconds} Seconds",
                            timestamp = LocalDateTime.now()
                        )
                        
                        // Handle the result
                        when (result) {
                            is ApiTaskResult.Success -> {
                                logger.info("Task $taskName Completed Successfully - " +
                                        "Task Will Run Again At $nextExecutionTime")
                                config.task.saveResult(result)
                            }
                            is ApiTaskResult.Error -> {
                                handleTaskError(config, result)
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Unexpected Error Executing Task $taskName", e)
                        val result = ApiTaskResult.Error(
                            taskName = taskName,
                            errorMessage = "Unexpected Error: ${e.message}",
                            timestamp = LocalDateTime.now()
                        )
                        handleTaskError(config, result)
                    } finally {
                        runningTasks.remove(taskName)
                    }
                }
                
                runningTasks[taskName] = job
            }
        }
    }
    
    /**
     * Handle a task error, potentially retrying the task.
     */
    private suspend fun handleTaskError(config: TaskConfig, result: ApiTaskResult.Error) {
        val now = LocalDateTime.now()
        val taskName = config.task.name
        val currentRetries = taskRetries.getOrDefault(taskName, 0)
        val nextExecutionTime = config.getNextExecutionTime(now)
        
        if (currentRetries < config.maxRetries) {
            val nextRetry = currentRetries + 1
            taskRetries[taskName] = nextRetry
            
            logger.warn("Task $taskName Failed, Scheduling Retry " +
                    "$nextRetry/${config.maxRetries} After ${config.retryDelay.seconds} Seconds")
            
            // Save the error result
            config.task.saveResult(result)
            
            // Schedule retry
            delay(config.retryDelay.toMillis())
            
            // Retry the task
            val retryResult = try {
                withTimeoutOrNull(config.timeout.toMillis()) {
                    config.task.execute(config.parameters)
                } ?: ApiTaskResult.Error(
                    taskName = taskName,
                    errorMessage = "Retry Timed Out After ${config.timeout.seconds} Seconds",
                    timestamp = LocalDateTime.now()
                )
            } catch (e: Exception) {
                ApiTaskResult.Error(
                    taskName = taskName,
                    errorMessage = "Error During Retry: ${e.message}",
                    timestamp = LocalDateTime.now()
                )
            }
            
            // Save the retry result
            config.task.saveResult(retryResult)
            
            if (retryResult is ApiTaskResult.Error && nextRetry < config.maxRetries) {
                // Continue with more retries if needed
                handleTaskError(config, retryResult)
            }
        } else {
            logger.error("Task $taskName Failed After ${config.maxRetries} Retries -" +
                    "Task Will Try Again At $nextExecutionTime")
            config.task.saveResult(result)
        }
    }
}