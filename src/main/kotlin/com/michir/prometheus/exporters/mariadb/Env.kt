package com.michir.prometheus.exporters.mariadb

import org.apache.log4j.Logger
import java.util.*


class Env {

    private val logger: Logger = Logger.getLogger(Env::class.java)

    private var properties: Properties = Properties()

    fun load(): Env {
        Env::class.java.getResourceAsStream("/application.properties")
            ?.use { properties.load(it) }

        System.getenv("ACTIVE_PROFILE")?.let {
            logger.info("active profile $it")
            Env::class.java.getResourceAsStream("/application-$it.properties")
                ?.use { inputStream -> properties.load(inputStream) }
        }

        return this
    }

    fun get(key: String): String? {
        val propCapital = key.uppercase(Locale.getDefault()).replace(".", "_")
        return if (System.getenv(propCapital) != null)
            System.getenv(propCapital)
        else {
            return properties.getProperty(key)
        }
    }

    fun getProperties(key: String): Map<String, Any> {
        val keys = properties.filter { it.key.toString().startsWith(key) }
            .mapKeys { it.key.toString().substringAfter("$key.") }

        keys.entries.associate {
            val keyUppercase = it.key.uppercase(Locale.getDefault()).replace(".", "_")
            if (System.getenv(keyUppercase) != null) {
                keyUppercase to it.value
            } else {
                it.key to it.value
            }
        }

        return keys
    }
}
