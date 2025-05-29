package org.anahit.api

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.LocalDateTime

class TrendingNewsArticles : BaseApiTask() {
    override val taskId: Int = 1
    override val taskName: String = "TrendingNewsArticles"
    override val taskDescription: String = "Fetches the latest trending news articles from the newsapi.org"
    override val cronExpression: String = "0 0/1 * * * ?"
    override val parameters: Map<String, Any> = emptyMap()
    override val maxRetries: Int = 3
    override val retryDelay: Duration = Duration.ofMinutes(1)
    override val timeout: Duration = Duration.ofMinutes(5)
    private val runtime = LocalDateTime.now()
    private val apiKey = "8e57b6225f964253a6c9737ed851dc54"
    private val categories: List<String> = listOf("business", "technology", "science", "sports")
    private val pageSize: Int = 5

    @Serializable
    data class NewsApiResponse(
        val status: String,
        val totalResults: Int,
        @SerialName("articles")
        val articles: List<Article>,
    )

    @Serializable
    data class Article(
        val source: Source,
        val author: String?,
        val title: String,
        val description: String?,
        val url: String,
        @SerialName("urlToImage")
        val imageUrl: String?,
        @SerialName("publishedAt")
        val publishedAt: String,
        val content: String?,
    )

    @Serializable
    data class Source(
        val id: String?,
        val name: String,
    )

    override suspend fun checkExistingApiResult(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun execute(): Pair<ApiTaskResult, MutableList<Any>> {
        logger.info("Fetching News From NewsAPI.org")
        val client =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        },
                    )
                }
            }

        try {
            val apiResponses = mutableListOf<Any>()

            for (category in categories) {
                val response =
                    client.get(
                        "https://newsapi.org/v2/" +
                            "top-headlines",
                    ) {
                        parameter("category", category)
                        parameter("pageSize", pageSize)
                        header("X-Api-Key", apiKey)
                    }

                if (response.status == HttpStatusCode.OK) {
                    val newsApiResponse = response.body<NewsApiResponse>()
                    apiResponses.add(newsApiResponse)
                } else {
                    logger.error("Failed To Fetch News From NewsAPI.org")
                    return Pair(
                        ApiTaskResult.Error(
                            taskId = taskId,
                            apiTaskRunName = "$now-$taskName",
                            apiTaskRunStatus = "Success",
                            apiTaskRunExecutedAt = runtime,
                        ),
                        apiResponses,
                    )
                }
            }
            return Pair(
                ApiTaskResult.Success(
                    taskId = taskId,
                    apiTaskRunName = "$now-$taskName",
                    apiTaskRunStatus = "Success",
                    apiTaskRunExecutedAt = runtime,
                ),
                apiResponses,
            )
        } catch (exception: Exception) {
            logger.error("Failed To Fetch News From NewsAPI.org", exception)
            return Pair(
                ApiTaskResult.Error(
                    taskId = taskId,
                    apiTaskRunName = "$now-$taskName",
                    apiTaskRunStatus = "Failed",
                    apiTaskRunExecutedAt = runtime,
                ),
                second = mutableListOf(),
            )
        }
    }

    override suspend fun saveApiResults(
        results: Pair<
            ApiTaskResult,
            MutableList<Any>,
        >,
    ): Boolean {
        println("Saving Result")
        return true
    }
}
