package org.anahit.config

import io.github.cdimascio.dotenv.dotenv

class EnvironmentConfig {
    val dotenv =
        dotenv {
            directory = "./src/main/resources"
            ignoreIfMalformed = false
            ignoreIfMissing = false
        }
}
