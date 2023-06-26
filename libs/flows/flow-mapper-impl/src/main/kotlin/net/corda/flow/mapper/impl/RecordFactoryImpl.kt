package net.corda.flow.mapper.impl

import net.corda.avro.serialization.CordaAvroSerializationFactory
import net.corda.avro.serialization.CordaAvroSerializer
import net.corda.data.flow.event.SessionEvent
import net.corda.data.flow.event.mapper.FlowMapperEvent
import net.corda.data.flow.state.mapper.FlowMapperState
import net.corda.data.p2p.app.AppMessage
import net.corda.flow.mapper.factory.RecordFactory
import net.corda.flow.mapper.impl.executor.createP2PRecord
import net.corda.flow.mapper.impl.executor.getSessionEventOutputTopic
import net.corda.libs.configuration.SmartConfig
import net.corda.membership.locally.hosted.identities.LocallyHostedIdentitiesService
import net.corda.messaging.api.records.Record
import net.corda.virtualnode.toCorda
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import java.time.Instant

@Component(service = [RecordFactory::class])
class RecordFactoryImpl @Activate constructor(
    @Reference(service = CordaAvroSerializationFactory::class)
    private val cordaAvroSerializationFactory: CordaAvroSerializationFactory,
    @Reference(service = LocallyHostedIdentitiesService::class)
    private val locallyHostedIdentitiesService: LocallyHostedIdentitiesService,
    private val appMessageFactory: (SessionEvent, CordaAvroSerializer<SessionEvent>, SmartConfig) -> AppMessage
): RecordFactory{

    private val sessionEventSerializer = cordaAvroSerializationFactory.createAvroSerializer<SessionEvent> { }

    override fun makeRecord(
        eventKey: String,
        sessionEvent: SessionEvent,
        flowMapperState: FlowMapperState?,
        instant: Instant,
        flowConfig: SmartConfig,
        event: SessionEvent
    ): Record<*, *> {

        val messageDirection = sessionEvent.messageDirection
        val outputTopic = getSessionEventOutputTopic(messageDirection)
        val holdingIdentity = (sessionEvent.initiatingIdentity).toCorda()

        return when (locallyHostedIdentitiesService.getIdentityInfo(holdingIdentity)) {
            null -> {
                Record(outputTopic, sessionEvent.sessionId, FlowMapperEvent(event))
            } else -> {
                createP2PRecord(
                    sessionEvent,
                    sessionEvent.payload,
                    instant,
                    sessionEventSerializer,
                    appMessageFactory,
                    flowConfig,
                    0
                )
            }
        }
    }
}