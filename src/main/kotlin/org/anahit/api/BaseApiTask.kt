package org.anahit.api

import kotlinx.serialization.json.Json
import org.anahit.config.TaskConfig
import org.anahit.logging.Logger
import org.anahit.models.ApiTaskRunsTable
import org.anahit.models.ApiTaskTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

/**
 * Base class for API tasks that provides common functionality.
 */
abstract class BaseApiTask : ApiTask {
    protected val logger = Logger.getLogger(this::class.java.name)
    protected val now: LocalDate? = LocalDate.now()
    protected val defaultJson =
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }

    /**
     * Checks if an API task run result already exists in the database for a given task name.
     * The check is based on a unique identifier constructed using the current timestamp and task name.
     *
     * @param taskName The name of the task whose existence in the database needs to be verified.
     * @return A Boolean value indicating whether the API task run result exists.
     * Returns true if it exists, false otherwise, or in case of an error.
     */
    override suspend fun checkExistingApiResult(taskName: String): Boolean {
        try {
            val searchName = "$now-$taskName"

            return transaction {
                ApiTaskRunsTable
                    .select { ApiTaskRunsTable.apiTaskRunName eq searchName }
                    .count() > 0
            }
        } catch (e: Exception) {
            // Log the error and return false to allow the API call as a fallback
            println("Error checking task runs: ${e.message}")
            return false
        }
    }

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
            transaction {
                val existingTask =
                    ApiTaskTable
                        .select { ApiTaskTable.apiTaskId eq taskConfig.taskId }
                        .singleOrNull()

                val shouldInsert =
                    if (existingTask != null) {
                        // Compare all fields to check if there are any changes
                        existingTask[ApiTaskTable.apiTaskName] != taskConfig.taskName ||
                            existingTask[ApiTaskTable.apiTaskDescription] != taskConfig.taskDescription ||
                            existingTask[ApiTaskTable.apiTaskCronExpression] != taskConfig.cronExpression ||
                            existingTask[ApiTaskTable.apiTaskMaxRetries] != taskConfig.maxRetries ||
                            existingTask[ApiTaskTable.apiTaskRetryDelay] != taskConfig.retryDelay.toMinutes().toInt() ||
                            existingTask[ApiTaskTable.apiTaskTimeout] != taskConfig.timeout.toMinutes().toInt()
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
                        it[apiTaskCronExpression] = taskConfig.cronExpression
                        it[apiTaskMaxRetries] = taskConfig.maxRetries
                        it[apiTaskRetryDelay] = taskConfig.retryDelay.toMinutes().toInt()
                        it[apiTaskTimeout] = taskConfig.timeout.toMinutes().toInt()
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
            transaction {
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
