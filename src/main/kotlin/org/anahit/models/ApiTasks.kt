package org.anahit.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Table for storing API tasks.
 */
object ApiTaskTable : Table("logging.api_tasks") {
    val apiTaskId = integer("api_task_id")
    val apiTaskName = varchar("api_task_name", 255)
    val apiTaskDescription = text("api_task_description")
    val apiTaskLastUpdatedAt = date("api_task_last_updated_at").nullable()
    val apiTaskCronExpression = varchar("api_task_cron_expression", 255)
    val apiTaskMaxRetries = integer("api_task_max_retries")
    val apiTaskRetryDelay = integer("api_task_retry_delay")
    val apiTaskTimeout = integer("api_task_timeout")

    override val primaryKey = PrimaryKey(apiTaskId)
}

/**
 * Table for storing API task results.
 */
object ApiTaskRunsTable : Table("logging.api_task_runs") {
    val apiTaskRunId = integer("api_task_run_id").autoIncrement()
    val apiTaskId = integer("api_task_id").references(ApiTaskTable.apiTaskId)
    val apiTaskRunName = varchar("api_task_run_name", 255)
    val apiTaskRunStatus = varchar("api_task_run_status", 50)
    val apiTaskRunExecutedAt = datetime("api_task_run_executed_at")

    override val primaryKey = PrimaryKey(apiTaskRunId)
}
