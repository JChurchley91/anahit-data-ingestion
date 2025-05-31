package org.anahit.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.anahit.logging.Logger
import org.anahit.models.ApiTaskRunsTable
import org.anahit.models.ApiTaskTable
import org.anahit.models.TrendingNewsArticlesTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

/**
 * Application configuration class responsible for setting up database connections
 * and initializing database models.
 */
class AppConfig(
    private val dbUrl: String,
    private val dbUser: String,
    private val dbPassword: String,
    private val dbDriver: String,
    private val maxPoolSize: Int,
) {
    private val logger = Logger.getLogger(this::class.java.name)
    private lateinit var dataSource: DataSource

    /**
     * Initialize the application configuration.
     * This sets up the database connection and initializes the database schema.
     */
    suspend fun initialize() {
        logger.info("Initializing Application Configuration")
        setupDatabase()
        initializeDatabaseSchema()
        logger.info("Application Configuration Initialized Successfully")
    }

    /**
     * Object containing all database schemas.
     */
    object Schemas {
        const val CONFIG = "config"
        const val LOGGING = "logging"
        const val RAW = "raw"
        const val TEST = "test"
        val all = arrayOf(CONFIG, LOGGING, RAW, TEST)
    }

    /**
     * Object containing all database tables.
     */
    object Tables {
        val apiTasks = ApiTaskTable
        val apiTaskResults = ApiTaskRunsTable
        val trendingNewsArticles = TrendingNewsArticlesTable
        val all = arrayOf(apiTasks, apiTaskResults, trendingNewsArticles)
    }

    /**
     * Set up the database connection using HikariCP.
     */
    private fun setupDatabase() {
        logger.info("Setting Up Database Connection To $dbUrl")

        val config =
            HikariConfig().apply {
                jdbcUrl = dbUrl
                username = dbUser
                password = dbPassword
                driverClassName = dbDriver
                maximumPoolSize = maxPoolSize
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        logger.info("Database Connection Established Successfully")
    }

    /**
     * Initialize the database schema by creating all tables.
     */
    private suspend fun initializeDatabaseSchema() =
        withContext(Dispatchers.IO) {
            logger.info("Initializing Database Schemas & Tables")
            transaction {
                // Create all schemas and tables
                for (schemaName in Schemas.all) {
                    exec("CREATE SCHEMA IF NOT EXISTS $schemaName")
                }
                SchemaUtils.create(*Tables.all)
                commit()
            }
            logger.info("Database Schemas & Tables Initialized Successfully")
        }
}
