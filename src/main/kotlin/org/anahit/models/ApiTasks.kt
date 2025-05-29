package org.anahit.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

/**
 * Table for storing API tasks.
 */
object ApiTaskTable : Table("logging.api_tasks") {
    val apiTaskId = integer("api_task_id").autoIncrement()
    val apiTaskName = varchar("api_task_name", 255)
    val apiTaskDescription = text("api_task_description").nullable()
    val apiTaskCreatedAt = date("api_task_created_at")
    val apiTaskUpdatedAt = date("api_task_updated_at")

    override val primaryKey = PrimaryKey(apiTaskId)
}

/**
 * Table for storing API task results.
 */
object ApiTaskResultTable : Table("logging.api_task_results") {
    val apiTaskRunId = integer("api_task_run_id").autoIncrement()
    val apiTaskId = integer("api_task_id").references(ApiTaskTable.apiTaskId)
    val apiTaskRunName = varchar("api_task_run_name", 255)
    val apiTaskRunStatus = varchar("api_task_run_status", 50)
    val apiTaskRunData = text("api_task_run_data").nullable()
    val apiTaskRunErrorMessage = text("api_task_run_error_message").nullable()
    val apiTaskRunExecutedAt = date("api_task_run_executed_at")

    override val primaryKey = PrimaryKey(apiTaskRunId)
}
