import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.anahit.api.FirstApiTask
import org.anahit.api.ApiTaskResult
import org.anahit.api.BaseApiTask
import org.anahit.config.AppConfig
import org.anahit.config.TaskConfig
import org.anahit.logging.Logger
import org.anahit.scheduler.Scheduler
import java.time.Duration
import java.time.LocalDateTime

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
    val appDbConfig = AppConfig(
        dbUrl = dbConfig.property("url").getString(),
        dbUser = dbConfig.property("user").getString(),
        dbPassword = dbConfig.property("password").getString(),
        dbDriver = dbConfig.propertyOrNull("driver")?.getString() ?: "org.postgresql.Driver",
        maxPoolSize = dbConfig.propertyOrNull("maxPoolSize")?.getString()?.toInt() ?: 10
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
    // Example task - replace with actual tasks
    val firstTask = FirstApiTask()

    return listOf(
        TaskConfig(
            task = firstTask,
            cronExpression = firstTask.cronExpression,
            parameters = firstTask.parameters,
            maxRetries = firstTask.maxRetries,
            retryDelay = firstTask.retryDelay,
            timeout = firstTask.timeout
        )
    )
}
