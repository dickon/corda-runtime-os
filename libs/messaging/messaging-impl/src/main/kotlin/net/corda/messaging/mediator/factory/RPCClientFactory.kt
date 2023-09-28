package net.corda.messaging.mediator.factory

import net.corda.avro.serialization.CordaAvroSerializationFactory
import net.corda.messaging.api.mediator.MessagingClient
import net.corda.messaging.api.mediator.config.MessagingClientConfig
import net.corda.messaging.api.mediator.factory.MessagingClientFactory
import net.corda.messaging.mediator.RPCClient
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference

@Component(service = [RPCClientFactory::class])
class RPCClientFactory(
    private val id: String,
): MessagingClientFactory {

    @Reference
    lateinit var cordaSerializationFactory: CordaAvroSerializationFactory
    override fun create(config: MessagingClientConfig): MessagingClient {
        return RPCClient(
            id,
            cordaSerializationFactory,
            config.onSerializationError
        )
    }
}