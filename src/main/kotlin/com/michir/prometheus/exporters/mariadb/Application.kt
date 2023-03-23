package com.michir.prometheus.exporters.mariadb

import com.sun.net.httpserver.HttpServer
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.sql.Connection
import java.sql.DriverManager
import java.util.function.Supplier

class Application

val appConfig = Env().load()
val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
val logger = LogHelper.handle(appConfig, Application::class.java)

fun main() = runBlocking {

    val dbConfig = appConfig.getProperties("maria.properties")
    Class.forName(dbConfig["driver"] as String);
    val connection = DriverManager.getConnection(
        dbConfig["url"] as String,
        dbConfig["username"] as String,
        dbConfig["password"] as String
    )

    appConfig.getProperties("gauges").forEach { (k, v) ->
        logger.info("Creating gauge for ($k, $v)")
        GaugeSupplier()
            .apply { Gauge.builder(k, this).register(registry) }
            .apply { startMetric(connection, v as String) {value -> this.value = value} }
    }

    appConfig.getProperties("counters").forEach { (k, v) ->
        logger.info("Creating counter for ($k, $v)")
        Counter.builder(k).register(registry)
            .apply { startMetric(connection, sqlStatement = v as String, scrapInterval = 3_000L) {
                this.increment(it.toDouble())
            } }
    }

    val server = HttpServer.create(InetSocketAddress(8080), 0)
        .apply { this.createContext(appConfig.get("monitoring.context_path")) {
            if ("GET" == it.requestMethod.uppercase()) {
                val response = registry.scrape().toByteArray()

                it.sendResponseHeaders(200, response.size.toLong())
                it.responseBody.write(response)
                it.close()
            }
        } }
        .apply { Runtime.getRuntime().addShutdownHook(Thread { this.stop(3_000) }) }

    server.start()
}

suspend fun startMetric(connection: Connection, sqlStatement: String, scrapInterval: Long = 3000L, fn: (value: Long) -> Unit) {
    GlobalScope.launch {
        while (true) {
            connection.createStatement()
                .let { statement ->
                    val resultSet = statement.executeQuery(sqlStatement)
                    while (resultSet.next()) {
                        fn.invoke(resultSet.getLong(1))
                    }
                }
            delay(scrapInterval)
        }
    }
}

class GaugeSupplier(var value: Number = 0) : Supplier<Number> {
    override fun get(): Number = value
}