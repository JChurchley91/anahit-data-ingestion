package org.anahit.api

import org.anahit.logging.Logger
import java.time.LocalDateTime
import java.time.Duration

/**
 * Represents the result of an API task execution.
 */
sealed class ApiTaskResult {
    abstract val taskName: String
    abstract val timestamp: LocalDateTime

    /**
     * Successful API task execution with data.
     */
    data class Success(
        override val taskName: String,
        val data: Any,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : ApiTaskResult()

    /**
     * Failed API task execution with an error message.
     */
    data class Error(
        override val taskName: String,
        val errorMessage: String,
        override val timestamp: LocalDateTime = LocalDateTime.now()
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
    val taskName: String
    val cronExpression: String
    val parameters: Map<String, Any>
    val maxRetries: Int
    val retryDelay: Duration
    val timeout: Duration

    /**
     * Check if an API task result already exists for the given parameters.
     * This method should check the database for existing results and return true if a result exists.
     */
    suspend fun checkExistingApiResult(parameters: Map<String, Any>): Boolean = false
    
    /**
     * Execute the API task.
     * This method should contain the logic to call the API and process the response.
     * 
     * @param parameters Additional parameters for the task execution
     * @return Result of the API task execution
     */
    suspend fun execute(parameters: Map<String, Any>): ApiTaskResult
    
    /**
     * Save the result of the API task to the database.
     * 
     * @param result The result to save
     */
    suspend fun saveResult(result: ApiTaskResult): Boolean
}

/**
 * Base class for API tasks that provides common functionality.
 */
abstract class BaseApiTask : ApiTask {
    protected val logger = Logger.getLogger(this::class.java.name)
}

