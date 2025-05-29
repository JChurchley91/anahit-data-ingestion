package org.anahit.logging

import org.slf4j.LoggerFactory

/**
 * Logger utility class that wraps SLF4J for logging.
 */
class Logger private constructor(
    private val logger: org.slf4j.Logger,
) {
    companion object {
        /**
         * Get a logger instance for the specified name.
         *
         * @param name The name of the logger
         * @return A Logger instance
         */
        fun getLogger(name: String): Logger = Logger(LoggerFactory.getLogger(name))
    }

    /**
     * Log a message at the INFO level.
     *
     * @param message The message to log
     */
    fun info(message: String) {
        logger.info(message)
    }

    /**
     * Log a message at the WARN level.
     *
     * @param message The message to log
     */
    fun warn(message: String) {
        logger.warn(message)
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param message The message to log
     * @param throwable Optional throwable to log
     */
    fun error(
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            logger.error(message, throwable)
        } else {
            logger.error(message)
        }
    }
}
