package net.corda.introspiciere.server

import io.javalin.Javalin
import io.javalin.http.InternalServerErrorResponse
import net.corda.introspiciere.core.HelloWorld
import net.corda.introspiciere.core.SimpleKafkaClient
import net.corda.introspiciere.core.addidentity.CreateKeysAndAddIdentityInteractor
import net.corda.introspiciere.core.addidentity.CryptoKeySenderImpl
import java.io.Closeable
import java.net.BindException
import java.net.ServerSocket

fun main() {
    IntrospiciereServer().start()
}

class IntrospiciereServer(private val port: Int = 0, private val kafkaBrokers: List<String>? = null) : Closeable {

    private lateinit var app: Javalin

    fun start() {
        val thePort = if (port > 0) port else availablePort(7070)

        app = Javalin.create().start(thePort)
        val servers = kafkaBrokers
            ?: System.getenv("KAFKA_BROKERS")?.split(",")
            ?: listOf("alpha-bk-1:9092")
        val kafka = SimpleKafkaClient(servers)

        app.get("/helloworld") { ctx ->
            wrapException {
                val greeting = HelloWorld().greeting()
                ctx.result(greeting)
            }
        }

        app.get("/topics") { ctx ->
            wrapException {
                val topics = kafka.fetchTopics()
                ctx.result(topics)
            }
        }

        app.post("/identities") { ctx ->
            wrapException {
                val input = ctx.bodyAsClass<CreateKeysAndAddIdentityInteractor.Input>()
                CreateKeysAndAddIdentityInteractor(CryptoKeySenderImpl(kafka)).execute(input)
                ctx.result("OK")
            }
        }
    }

    override fun close() {
        if (::app.isInitialized) app.close()
    }

    private fun availablePort(startingPort: Int): Int {
        var port = startingPort
        while (true) {
            try {
                ServerSocket(port).close()
                return port
            } catch (ex: BindException) {
                port = (port + 1) % 65_535
            }
        }
    }

    private fun <R> wrapException(action: () -> R): R {
        try {
            return action()
        } catch (t: Throwable) {
            throw InternalServerErrorResponse(details = mapOf("Exception" to t.stackTraceToString()))
        }
    }
}
