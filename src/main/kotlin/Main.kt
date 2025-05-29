import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.anahit.api.TrendingNewsArticles
import org.anahit.config.AppConfig
import org.anahit.config.TaskConfig
import org.anahit.logging.Logger
import org.anahit.scheduler.Scheduler

/**
 * Main entry point for the application.
 */
fun main(args: Array<String>) {
    val logger = Logger.getLogger("Main")
    logger.info("Starting Anahit Data Ingestion Service")

    // Start the Ktor server using application.conf
    EngineMain.main(args)
}

/**
 * Application module configuration.
 */
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val logger = Logger.getLogger("Main")

    // Get configuration from application.conf
    val appConfig = environment.config

    // Configure database
    val dbConfig = appConfig.config("database")

    val appDbConfig =
        AppConfig(
            dbUrl = dbConfig.property("url").getString(),
            dbUser = dbConfig.property("user").getString(),
            dbPassword = dbConfig.property("password").getString(),
            dbDriver = dbConfig.propertyOrNull("driver")?.getString() ?: "org.postgresql.Driver",
            maxPoolSize = dbConfig.propertyOrNull("maxPoolSize")?.getString()?.toInt() ?: 10,
        )

    // Configure scheduler
    val schedulerConfig = appConfig.config("scheduler")
    val checkInterval = schedulerConfig.propertyOrNull("checkInterval")?.getString()?.toLong() ?: 1000L

    // Configure routing
    configureRouting()

    // Initialize the application when it starts
    environment.monitor.subscribe(ApplicationStarted) {
        runBlocking {
            try {
                // Initialize the database
                appDbConfig.initialize()

                // Create and configure tasks
                val tasks = createTasks()

                // Start the scheduler
                val scheduler = Scheduler(tasks, checkInterval)
                scheduler.start()

                logger.info("Application Initialized Successfully")
            } catch (exception: Exception) {
                logger.error("Failed To Initialize Application", exception)
                throw exception
            }
        }
    }
}

/**
 * Configure the Ktor routing.
 */
fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respondText("OK")
        }
    }
}

/**
 * Create and configure the tasks to be scheduled.
 */
private fun createTasks(): List<TaskConfig> {
    val trendingNewsArticles = TrendingNewsArticles()

    return listOf(
        TaskConfig(
            task = trendingNewsArticles,
            taskId = trendingNewsArticles.taskId,
            taskName = trendingNewsArticles.taskName,
            taskDescription = trendingNewsArticles.taskDescription,
            cronExpression = trendingNewsArticles.cronExpression,
            maxRetries = trendingNewsArticles.maxRetries,
            retryDelay = trendingNewsArticles.retryDelay,
            timeout = trendingNewsArticles.timeout,
        ),
    )
}
