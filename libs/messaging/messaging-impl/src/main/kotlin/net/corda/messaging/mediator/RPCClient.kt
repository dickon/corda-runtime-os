package net.corda.messaging.mediator

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.corda.avro.serialization.CordaAvroSerializationFactory
import net.corda.avro.serialization.CordaAvroSerializer
import net.corda.data.flow.event.external.ExternalEvent
import net.corda.messaging.api.mediator.MediatorMessage
import net.corda.messaging.api.mediator.MessagingClient
import org.osgi.service.component.annotations.Reference
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RPCClient(
    override val id: String,
    cordaAvroSerializerFactory: CordaAvroSerializationFactory,
    private val onSerializationError: ((ByteArray) -> Unit)?,
    private val httpClientFactory: () -> HttpClient = { HttpClient.newBuilder().build() }
) : MessagingClient {
    private val httpClient: HttpClient = httpClientFactory()
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // TODO: Confirm types for serdes
    private val serializer = cordaAvroSerializerFactory.createAvroSerializer<Any> {}
    private val deserializer = cordaAvroSerializerFactory.createAvroDeserializer({}, Record::class.java)

    private val httpExceptions = setOf(
        IOException::class,
        InterruptedException::class,
        HttpTimeoutException::class,
        IllegalArgumentException::class,
        SecurityException::class
    )

    private val coroutineExceptions = setOf(
        IllegalStateException::class,
        CancellationException::class
    )

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun send(message: MediatorMessage<*>): Deferred<MediatorMessage<*>?> {
        val deferred = CompletableDeferred<MediatorMessage<*>?>()

        scope.launch {
            try {
                val result = processMessage(message)
                deferred.complete(result)
            } catch (e: Exception) {
                handleExceptions(e, deferred)
            }
        }

        return deferred;
    }

    private fun processMessage(message: MediatorMessage<*>): MediatorMessage<*> {
        val payload = serializer.serialize(message.payload as Record)

        val request = HttpRequest.newBuilder()
            .uri(URI("corda-flow-mapper-worker-cluster-ip-service:7000"))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(payload))
            .build()

        // TODO Handle actual response type of Record<String, FlowEvent> (topic: topic, key: flowId, value: FlowEvent)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())

        // TODO Add handling for various status codes
        val deserializedResponse = deserializer.deserialize(response.body())

        return MediatorMessage(deserializedResponse, mutableMapOf("statusCode" to response.statusCode()))
    }

    // TODO This is placeholder exception handling
    private fun handleExceptions(e: Exception, deferred: CompletableDeferred<MediatorMessage<*>?>) {
        when (e::class) {
            in httpExceptions -> log.error("Something went wrong while sending external event via RPC: $e")
            in coroutineExceptions -> log.error("Something went wrong while launching coroutine in RPCClient: $e")
            else -> log.error("Unhandled exception in RPCClient: $e")
        }

        // Temp
        onSerializationError?.invoke("hello".toByteArray())

        deferred.completeExceptionally(e)
    }

    override fun close() {
        job.cancel()
    }
}
