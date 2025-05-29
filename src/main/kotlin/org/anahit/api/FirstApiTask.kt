package org.anahit.api

class FirstApiTask : BaseApiTask() {
    override val taskName: String = "FirstApiTask"
    override val cronExpression: String = "0 0/1 * * * ?"
    override val parameters: Map<String, Any> = emptyMap()
    override val maxRetries: Int = 3
    override val retryDelay: java.time.Duration = java.time.Duration.ofMinutes(1)
    override val timeout: java.time.Duration = java.time.Duration.ofMinutes(5)

    override suspend fun saveResult(result: ApiTaskResult): Boolean {
        logger.info("Saving result of task $taskName to database")
        return true
    }

    override suspend fun checkExistingApiResult(parameters: Map<String, Any>): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun execute(parameters: Map<String, Any>): ApiTaskResult {
        logger.info("Executing task $taskName")
        return ApiTaskResult.Success(taskName, "TEST")
    }
}
