package org.anahit.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object TrendingNewsArticlesTable : Table("raw.trending_news_articles") {
    val trendingNewsArticleId = integer("trending_news_article_id").autoIncrement()
    val apiTaskId = integer("api_task_id").references(ApiTaskTable.apiTaskId)
    val apiTaskRunName = varchar("api_task_run_name", 255)
    val articleSourceName = varchar("article_source_name", 255)
    val articleTitle = varchar("article_title", 255)
    val articleCountry = varchar("article_country", 255)
    val articleCategory = varchar("article_category", 255)
    val articleDescription = text("article_description")
    val articleUrl = varchar("article_url", 255)
    val articlePublishedAt = varchar("article_published_at", 255)
    val articleSavedAt = datetime("article_saved_at")

    override val primaryKey = PrimaryKey(trendingNewsArticleId)
}
