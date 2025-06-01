package org.anahit.api

import java.time.LocalDateTime

/**
 * Represents the result of an API task execution.
 */
abstract class ApiTaskResult {
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