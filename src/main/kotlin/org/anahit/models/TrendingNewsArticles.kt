package org.anahit.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object TrendingNewsArticlesTable : Table("raw.trending_news_articles") {
    val trendingNewsArticleId = integer("trending_news_article_id").autoIncrement()
    val apiTaskId = integer("api_task_id").references(ApiTaskTable.apiTaskId)
    val apiTaskRunName = varchar("api_task_run_name", 255)
    val articleSourceName = varchar("source_name", 255)
    val articleTitle = varchar("title", 255)
    val articleDescription = text("description")
    val articleUrl = varchar("url", 255)
    val articlePublishedAt = varchar("published_at", 255)
    val articleSavedAt = datetime("saved_at")

    override val primaryKey = PrimaryKey(trendingNewsArticleId)
}
