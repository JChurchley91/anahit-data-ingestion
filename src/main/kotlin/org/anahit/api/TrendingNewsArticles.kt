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
import org.anahit.models.TrendingNewsArticlesTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
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
    private val taskRunTime = LocalDateTime.now()
    private val apiKey = "8e57b6225f964253a6c9737ed851dc54"

    val defaultJson =
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }

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
     * @property title The title of the article.
     * @property description A brief description or summary of the article, if available.
     * @property url The direct URL to access the full article.
     * @property publishedAt The timestamp of when the article was published.
     */
    @Serializable
    data class Article(
        val source: Source,
        val title: String?,
        val description: String?,
        val url: String?,
        @SerialName("publishedAt")
        val publishedAt: String?,
    )

    @Serializable
    data class Source(
        val id: String?,
        val name: String?,
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
                    apiTaskRunExecutedAt = taskRunTime,
                ),
                mutableListOf(),
            )
        } else {
            logger.info("Fetching News From NewsAPI.org")
            val client =
                HttpClient(CIO) {
                }
            try {
                val apiResponses = mutableListOf<Any>()
                val response =
                    client.get(
                        "https://newsapi.org/v2/top-headlines" +
                            "?country=us&apiKey=$apiKey",
                    )
                val responseBody: String = response.body()

                if (response.status == HttpStatusCode.OK) {
                    val newsApiResponse: NewsApiResponse = defaultJson.decodeFromString(responseBody)
                    apiResponses.add(newsApiResponse)
                } else {
                    logger.error("Failed To Fetch News From NewsAPI.org")
                    return Pair(
                        ApiTaskResult.Error(
                            taskId = taskId,
                            apiTaskRunName = "$now-$taskName",
                            apiTaskRunStatus = "Failed",
                            apiTaskRunExecutedAt = taskRunTime,
                        ),
                        apiResponses,
                    )
                }
                return Pair(
                    ApiTaskResult.Success(
                        taskId = taskId,
                        apiTaskRunName = "$now-$taskName",
                        apiTaskRunStatus = "Success",
                        apiTaskRunExecutedAt = taskRunTime,
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
                        apiTaskRunExecutedAt = taskRunTime,
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
        try {
            val apiResponses: MutableList<NewsApiResponse> =
                results.second
                    .filterIsInstance<NewsApiResponse>()
                    .toMutableList()

            if (apiResponses.isEmpty()) {
                logger.info("No News API Responses Found; Skipping Save")
                return true
            } else {
                val top10Articles = apiResponses.flatMap { it.articles }.take(10)
                for (article in top10Articles) {
                    transaction {
                        TrendingNewsArticlesTable.insert {
                            it[apiTaskId] = taskId
                            it[apiTaskRunName] = results.first.apiTaskRunName
                            it[articleSourceName] = article.source.name.toString()
                            it[articleTitle] = article.title.toString()
                            it[articleDescription] = article.description.toString()
                            it[articleUrl] = article.url.toString()
                            it[articlePublishedAt] = article.publishedAt.toString()
                            it[articleSavedAt] = taskRunTime
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            logger.error("Failed To Save News API Responses", exception)
            return false
        }
        return true
    }
}
