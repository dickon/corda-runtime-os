package net.corda.flow.mapper.impl

import net.corda.avro.serialization.CordaAvroSerializationFactory
import net.corda.data.ExceptionEnvelope
import net.corda.data.flow.event.FlowEvent
import net.corda.data.flow.event.MessageDirection
import net.corda.data.flow.event.SessionEvent
import net.corda.data.flow.event.mapper.FlowMapperEvent
import net.corda.data.flow.event.session.SessionError
import net.corda.data.identity.HoldingIdentity
import net.corda.data.p2p.app.AppMessage
import net.corda.data.p2p.app.AuthenticatedMessage
import net.corda.data.p2p.app.AuthenticatedMessageHeader
import net.corda.data.p2p.app.MembershipStatusFilter
import net.corda.flow.mapper.factory.RecordFactory
import net.corda.flow.utils.isInitiatedIdentity
import net.corda.libs.configuration.SmartConfig
import net.corda.membership.locally.hosted.identities.LocallyHostedIdentitiesService
import net.corda.messaging.api.records.Record
import net.corda.schema.Schemas
import net.corda.schema.configuration.FlowConfig
import net.corda.session.manager.Constants
import net.corda.virtualnode.toCorda
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import java.nio.ByteBuffer
import java.time.Instant
import java.util.UUID

@Component(service = [RecordFactory::class])
class RecordFactoryImpl @Activate constructor(
    @Reference(service = CordaAvroSerializationFactory::class)
    private val cordaAvroSerializationFactory: CordaAvroSerializationFactory,
    @Reference(service = LocallyHostedIdentitiesService::class)
    private val locallyHostedIdentitiesService: LocallyHostedIdentitiesService
): RecordFactory {
    private val sessionEventSerializer = cordaAvroSerializationFactory.createAvroSerializer<SessionEvent> { }

    override fun forwardError(
        sessionEvent: SessionEvent,
        exceptionEnvelope: ExceptionEnvelope,
        instant: Instant,
        flowConfig: SmartConfig,
        flowId: String,
        isInteropSession: Boolean
    ): Record<*, *> {
        return buildSessionRecord(
            sessionEvent,
            SessionError(
                exceptionEnvelope
            ),
            instant,
            flowConfig,
            flowId,
            isInteropSession
        )
    }

    override fun forwardEvent(
        sessionEvent: SessionEvent,
        instant: Instant,
        flowConfig: SmartConfig,
        flowId: String,
        isInteropSession: Boolean
    ): Record<*, *> {
        return buildSessionRecord(
            sessionEvent,
            sessionEvent.payload,
            instant,
            flowConfig,
            flowId,
            isInteropSession
        )
    }

    private fun getSessionEventOutputTopic(sessionEvent: SessionEvent, isInteropSession: Boolean): String {
        return when (sessionEvent.messageDirection) {
            MessageDirection.INBOUND -> Schemas.Flow.FLOW_EVENT_TOPIC
            MessageDirection.OUTBOUND -> {
                if (isInteropSession) {
                    Schemas.Flow.FLOW_INTEROP_EVENT_TOPIC
                } else {
                    if (isLocalCluster(sessionEvent)) {
                        Schemas.Flow.FLOW_MAPPER_EVENT_TOPIC
                    } else {
                        Schemas.P2P.P2P_OUT_TOPIC
                    }
                }
            }
            else -> {
                throw IllegalArgumentException("Session event had an invalid message direction set: ${sessionEvent.messageDirection}")
            }
        }
    }

    @Suppress("LongParameterList")
    private fun buildSessionRecord(
        sourceEvent: SessionEvent,
        newPayload: Any,
        timestamp: Instant,
        config: SmartConfig,
        flowId: String,
        isInteropSession: Boolean
    ) : Record<*, *> {
        val outputTopic = getSessionEventOutputTopic(sourceEvent, isInteropSession)
        val (newDirection, sessionId) = when (outputTopic) {
            Schemas.Flow.FLOW_MAPPER_EVENT_TOPIC -> Pair(MessageDirection.INBOUND, toggleSessionId(sourceEvent.sessionId))
            Schemas.Flow.FLOW_EVENT_TOPIC -> Pair(MessageDirection.INBOUND, sourceEvent.sessionId)
            else -> Pair(MessageDirection.OUTBOUND, sourceEvent.sessionId)
        }
        val sequenceNumber = if (newPayload is SessionError) null else sourceEvent.sequenceNum
        val sessionEvent = SessionEvent(
            newDirection,
            timestamp,
            sessionId,
            sequenceNumber,
            sourceEvent.initiatingIdentity,
            sourceEvent.initiatedIdentity,
            newPayload,
            sourceEvent.contextSessionProperties
        )
        return when (outputTopic) {
            Schemas.Flow.FLOW_EVENT_TOPIC -> {
                Record(outputTopic, flowId, FlowEvent(flowId, sessionEvent))
            }
            Schemas.Flow.FLOW_INTEROP_EVENT_TOPIC -> {
                Record(outputTopic, sessionId, FlowMapperEvent(sessionEvent))
            }
            Schemas.Flow.FLOW_MAPPER_EVENT_TOPIC -> {
                Record(outputTopic, sessionEvent.sessionId, FlowMapperEvent(sessionEvent))
            }
            Schemas.P2P.P2P_OUT_TOPIC -> {
                val appMessage = generateAppMessage(sessionEvent, config)
                Record(outputTopic, sessionId, appMessage)
            }
            else -> {
                throw IllegalArgumentException("Invalid output topic of $outputTopic was found when forwarding a session event")
            }
        }
    }

    /**
     * Data class for source and destination holding identities from a session event.
     */
    private data class SourceAndDestination(
        val sourceIdentity: HoldingIdentity,
        val destinationIdentity: HoldingIdentity
    )

    /**
     * Get the source and destination holding identity from the [sessionEvent].
     * @param sessionEvent Session event to extract identities from
     * @return Source and destination identities for a SessionEvent message.
     */
    private fun getSourceAndDestinationIdentity(sessionEvent: SessionEvent): SourceAndDestination {
        return if (isInitiatedIdentity(sessionEvent.sessionId)) {
            SourceAndDestination(sessionEvent.initiatedIdentity, sessionEvent.initiatingIdentity)
        } else {
            SourceAndDestination(sessionEvent.initiatingIdentity, sessionEvent.initiatedIdentity)
        }
    }

    private fun isLocalCluster(
        sessionEvent: SessionEvent
    ): Boolean {
        val destinationIdentity = getSourceAndDestinationIdentity(sessionEvent).destinationIdentity
        return when (locallyHostedIdentitiesService.getIdentityInfo(destinationIdentity.toCorda())) {
            null -> false
            else -> true
        }
    }

    /**
     * Generate an AppMessage to send to the P2P.out topic.
     * @param sessionEvent Flow event to send
     * @param flowConfig config
     * @return AppMessage to send to the P2P.out topic with the serialized session event as payload
     */
    private fun generateAppMessage(
        sessionEvent: SessionEvent,
        flowConfig: SmartConfig
    ): AppMessage {
        val (sourceIdentity, destinationIdentity) = getSourceAndDestinationIdentity(sessionEvent)
        val header = AuthenticatedMessageHeader(
            destinationIdentity,
            sourceIdentity,
            Instant.ofEpochMilli(sessionEvent.timestamp.toEpochMilli() + flowConfig.getLong(FlowConfig.SESSION_P2P_TTL)),
            sessionEvent.sessionId + "-" + UUID.randomUUID(),
            "",
            Constants.FLOW_SESSION_SUBSYSTEM,
            MembershipStatusFilter.ACTIVE
        )
        return AppMessage(AuthenticatedMessage(header, ByteBuffer.wrap(sessionEventSerializer.serialize(sessionEvent))))
    }

    /**
     * Toggle the [sessionId] to that of the other party and return it.
     * Initiating party sessionId will be a random UUID.
     * Initiated party sessionId will be the initiating party session id with a suffix of "-INITIATED" added.
     * @return the toggled session id
     */
    private fun toggleSessionId(sessionId: String): String {
        return if (sessionId.endsWith(Constants.INITIATED_SESSION_ID_SUFFIX)) {
            sessionId.removeSuffix(Constants.INITIATED_SESSION_ID_SUFFIX)
        } else {
            "$sessionId${Constants.INITIATED_SESSION_ID_SUFFIX}"
        }
    }
}

