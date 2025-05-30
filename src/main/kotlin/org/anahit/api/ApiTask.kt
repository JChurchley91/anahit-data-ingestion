package org.anahit.api

import org.anahit.config.TaskConfig
import org.anahit.logging.Logger
import org.anahit.models.ApiTaskRunsTable
import org.anahit.models.ApiTaskTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Represents the result of an API task execution.
 */
sealed class ApiTaskResult {
    abstract val taskId: Int
    abstract val apiTaskRunName: String
    abstract val apiTaskRunStatus: String
    abstract val apiTaskRunExecutedAt: LocalDateTime

    /**
     * Successful API task execution with data.
     */
    data class Success(
        override val taskId: Int,
        override val apiTaskRunName: String,
        override val apiTaskRunStatus: String,
        override val apiTaskRunExecutedAt: LocalDateTime,
    ) : ApiTaskResult()

    /**
     * Failed API task execution with an error message.
     */
    data class Error(
        override val taskId: Int,
        override val apiTaskRunName: String,
        override val apiTaskRunStatus: String,
        override val apiTaskRunExecutedAt: LocalDateTime,
    ) : ApiTaskResult()
}

/**
 * Interface for API tasks that can be scheduled and executed.
 * All API tasks should implement this interface.
 */
interface ApiTask {
    /**
     * The unique name of the task.
     */
    val taskId: Int
    val taskName: String
    val taskDescription: String
    val cronExpression: String
    val parameters: Map<String, Any>
    val maxRetries: Int
    val retryDelay: Duration
    val timeout: Duration

    /**
     * Check if API has already been called today.
     * Do not call the API again if so, to save credits.
     * @result boolean to confirm if the API call exists for today or not.
     */
    suspend fun checkExistingApiResult(): Boolean = false

    /**
     * Save the API task to ApiTaskTable.
     * @param taskConfig the taskconfig for the given task.
     * @return boolean to confirm if save successful.
     */
    suspend fun saveApiTask(taskConfig: TaskConfig): Boolean

    /**
     * Save the API task run to ApiTaskRunsTable.
     * @param results a pair containing the API task results.
     * @return boolean to confirm if save successful.
     */
    suspend fun saveApiTaskRun(
        results: Pair<
            ApiTaskResult,
            MutableList<Any>,
        >,
    ): Boolean

    /**
     * Execute the API task.
     * This method should contain the logic to call the API and process the response.
     * @return Result of the API task execution
     */
    suspend fun execute(): Pair<ApiTaskResult, MutableList<Any>>?

    /**
     * Save the result of the API task to the database.
     * Results should be saved in a table specific to the API call.
     * @param results The results to save to the given table.
     */
    suspend fun saveApiResults(
        results: Pair<
            ApiTaskResult,
            MutableList<Any>,
        >,
    ): Boolean
}

/**
 * Base class for API tasks that provides common functionality.
 */
abstract class BaseApiTask : ApiTask {
    protected val logger = Logger.getLogger(this::class.java.name)
    protected val now: LocalDate? = LocalDate.now()

    /**
     * Saves or updates the configuration of an API task in the database.
     * If the task with the given ID already exists and its properties differ,
     * it will be updated. Otherwise, a new record will be inserted.
     *
     * @param taskConfig The configuration of the task to be saved, including ID, name,
     * description, scheduling details, and execution parameters.
     * @return A Boolean value indicating whether the save operation was successful or not.
     * Returns true if the task was successfully saved or updated, false otherwise.
     */
    override suspend fun saveApiTask(taskConfig: TaskConfig): Boolean {
        try {
            org.jetbrains.exposed.sql.transactions.transaction {
                val existingTask =
                    ApiTaskTable
                        .select { ApiTaskTable.apiTaskId eq taskConfig.taskId }
                        .singleOrNull()

                val shouldInsert =
                    if (existingTask != null) {
                        // Compare all fields to check if there are any changes
                        existingTask[ApiTaskTable.apiTaskName] != taskConfig.taskName ||
                            existingTask[ApiTaskTable.apiTaskDescription] != taskConfig.taskDescription ||
                            existingTask[ApiTaskTable.cronExpression] != taskConfig.cronExpression ||
                            existingTask[ApiTaskTable.maxRetries] != taskConfig.maxRetries ||
                            existingTask[ApiTaskTable.retryDelay] != taskConfig.retryDelay.toMinutes().toInt() ||
                            existingTask[ApiTaskTable.timeout] != taskConfig.timeout.toMinutes().toInt()
                    } else {
                        // If no existing task found, we should insert
                        true
                    }

                if (shouldInsert) {
                    ApiTaskTable.insert {
                        it[apiTaskId] = taskConfig.taskId
                        it[apiTaskName] = taskConfig.taskName
                        it[apiTaskDescription] = taskConfig.taskDescription
                        it[apiTaskLastUpdatedAt] = now
                        it[cronExpression] = taskConfig.cronExpression
                        it[maxRetries] = taskConfig.maxRetries
                        it[retryDelay] = taskConfig.retryDelay.toMinutes().toInt()
                        it[timeout] = taskConfig.timeout.toMinutes().toInt()
                    }
                }
                true
            }
        } catch (exception: Exception) {
            logger.error("Failed To Save ApiTask", exception)
            false
        }
        return true
    }

    /**
     * Saves the result of an API task execution along with its associated data to the database.
     *
     * @param results A pair containing:
     *  - An [ApiTaskResult] representing the outcome of the task execution, including task ID, name,
     *    status, and execution timestamp.
     *  - A mutable list of objects that includes additional data or results associated with the task.
     * @return A Boolean value indicating whether the save operation was successful or not.
     * Returns true if the result was successfully saved; false otherwise.
     */
    override suspend fun saveApiTaskRun(
        results: Pair<
            ApiTaskResult,
            MutableList<Any>,
        >,
    ): Boolean {
        try {
            org.jetbrains.exposed.sql.transactions.transaction {
                ApiTaskRunsTable.insert {
                    it[apiTaskId] = results.first.taskId
                    it[apiTaskRunName] = results.first.apiTaskRunName
                    it[apiTaskRunStatus] = results.first.apiTaskRunStatus
                    it[apiTaskRunExecutedAt] = results.first.apiTaskRunExecutedAt
                }
            }
        } catch (exception: Exception) {
            logger.error("Failed To Save ApiTaskRun", exception)
            return false
        }
        return true
    }
}
