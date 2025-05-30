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

/**
 * Represents a task for fetching the latest trending news articles from the NewsAPI.org service.
 * This task is scheduled to run at defined intervals and retrieves news articles across multiple predefined categories.
 */
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

    /**
     * Represents the response from a news API, providing details about the status of the response,
     * the total number of results, and a list of articles.
     *
     * @property status The status of the API response (e.g., "ok", "error").
     * @property totalResults The total number of news articles available from the API query.
     * @property articles A list of news articles retrieved from the API.
     */
    @Serializable
    data class NewsApiResponse(
        val status: String,
        val totalResults: Int,
        @SerialName("articles")
        val articles: List<Article>,
    )

    /**
     * Represents a single news article with various metadata and content.
     *
     * @property source The source of the article, providing details about where it originated.
     * @property author The name of the author of the article, if available.
     * @property title The title of the article.
     * @property description A brief description or summary of the article, if available.
     * @property url The direct URL to access the full article.
     * @property imageUrl The URL of the image associated with the article, if available.
     * @property publishedAt The timestamp of when the article was published.
     * @property content The main content of the article, if available.
     */
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

    /**
     * Represents the source of a news article.
     *
     * @property id The unique identifier for the source, or null if the ID is unavailable.
     * @property name The name of the source.
     */
    @Serializable
    data class Source(
        val id: String?,
        val name: String,
    )

    /**
     * Executes the task to fetch news articles from the NewsAPI.org service based on defined categories.
     *
     * @return A pair containing an instance of [ApiTaskResult], which indicates the result of the API task execution,
     * and a mutable list of objects representing the responses from the API. In case of success, the list contains the
     * retrieved news articles. In case of failure, the list may be empty.
     */
    override suspend fun execute(): Pair<ApiTaskResult, MutableList<Any>> {
        if (checkExistingApiResult(taskName)) {
            logger.info("Task $taskName already executed today, skipping.")
            return Pair(
                ApiTaskResult.Success(
                    taskId = taskId,
                    apiTaskRunName = "$now-$taskName",
                    apiTaskRunStatus = "Skipped",
                    apiTaskRunExecutedAt = runtime,
                ),
                mutableListOf(),
            )
        } else {
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
