package com.michir.prometheus.exporters.mariadb

import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger

class LogHelper {

    companion object {
        fun handle(appConfig: Env, clazz: Class<*>): Logger {
            appConfig.get("log.level")?.let { LogManager.getRootLogger().level = Level.toLevel(it.uppercase()) }
            return Logger.getLogger(clazz)
        }
    }
}