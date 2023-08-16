package net.corda.rpc.server

import net.corda.avro.serialization.CordaAvroSerializationFactory
import io.javalin.Javalin
import io.javalin.http.Handler
import net.corda.applications.workers.workercommon.JavalinServer
import org.slf4j.LoggerFactory
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference


@Component(service = [RPCServer::class])
class RPCServerImpl @Activate constructor(
    @Reference(service = CordaAvroSerializationFactory::class)
    private val cordaAvroSerializationFactory: CordaAvroSerializationFactory,
    @Reference(service = JavalinServer::class)
    private val javalinServer: JavalinServer
) : RPCServer {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun <REQ: Any, RESP: Any> registerEndpoint(endpoint: String, handler: (REQ) -> RESP, clazz: Class<REQ>) {
        val server = javalinServer.getServer()
        if(server != null) {
            server.post(endpoint, Handler { ctx ->

                val avroDeserializer = cordaAvroSerializationFactory.createAvroDeserializer({
                    log.error("Failed to deserialize payload for request")
                    ctx.result("Failed to deserialize request payload")
                    ctx.res.status = 500;
                }, clazz)
                val avroSerializer = cordaAvroSerializationFactory.createAvroSerializer<RESP> {
                    log.error("Failed to serialize payload for response")
                    ctx.result("Failed to serialize response payload")
                    ctx.res.status = 500;
                }

                val payload = avroDeserializer.deserialize(ctx.bodyAsBytes())

                if (payload != null) {
                    val serializedResponse = avroSerializer.serialize(handler(payload))
                    if (serializedResponse != null) {
                        ctx.result(serializedResponse)
                    } else {
                        ctx.result("Response Payload was Null")
                        ctx.res.status = 422
                    }
                } else {
                    ctx.result("Request Payload was Null")
                    ctx.res.status = 422
                }
            })
        } else {
            throw Exception("The Javalin Server must be initialized before routes are added")
        }
    }
}