package org.anahit.api

import org.anahit.config.TaskConfig
import java.time.Duration

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
    suspend fun checkExistingApiResult(taskName: String): Boolean = false

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
